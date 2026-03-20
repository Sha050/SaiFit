from pathlib import Path
import subprocess

import cv2
import imageio_ffmpeg
import numpy as np
from ultralytics import YOLO

KP_L_SHOULDER = 5
KP_R_SHOULDER = 6
KP_L_HIP = 11
KP_R_HIP = 12
KP_L_KNEE = 13
KP_R_KNEE = 14
KP_L_ANKLE = 15
KP_R_ANKLE = 16

MODEL_PATH = Path(__file__).resolve().parents[2] / "yolov8m-pose.pt"
INFERENCE_IMAGE_SIZE = 512
MIN_KEYPOINT_CONFIDENCE = 0.35

_model = None


def _get_model():
    global _model
    if _model is None:
        if not MODEL_PATH.exists():
            raise FileNotFoundError(f"YOLO model not found at {MODEL_PATH}")
        print(f"[pose_evaluator] Loading YOLOv8m-Pose model from {MODEL_PATH}...")
        _model = YOLO(str(MODEL_PATH))
        print("[pose_evaluator] YOLOv8m-Pose model loaded successfully.")
    return _model


def calculate_angle(a, b, c):
    a = np.array(a, dtype=np.float64)
    b = np.array(b, dtype=np.float64)
    c = np.array(c, dtype=np.float64)
    ba = a - b
    bc = c - b
    cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-8)
    cosine = np.clip(cosine, -1.0, 1.0)
    return float(np.degrees(np.arccos(cosine)))


def _point(kps, idx):
    return [float(kps[idx][0]), float(kps[idx][1])]


def _avg_kp(kps, idx_a, idx_b):
    return [
        float((kps[idx_a][0] + kps[idx_b][0]) / 2.0),
        float((kps[idx_a][1] + kps[idx_b][1]) / 2.0),
    ]


def _distance(a, b):
    return float(np.hypot(a[0] - b[0], a[1] - b[1]))


def _kp_conf(kps, idx_a, idx_b):
    return float(min(kps[idx_a][2], kps[idx_b][2]))


def _frame_stride_for(fps: float, test_type: str) -> int:
    if test_type == "situps":
        target_analysis_fps = 6.0
    elif test_type == "vertical_jump":
        target_analysis_fps = 14.0
    elif test_type == "shuttle_run":
        target_analysis_fps = 12.0
    else:
        target_analysis_fps = 10.0
    return max(1, int(round(fps / target_analysis_fps)))


def _extract_keypoints(results):
    if not results or results[0].keypoints is None:
        return None

    keypoints_data = results[0].keypoints
    if keypoints_data.data is None or len(keypoints_data.data) == 0:
        return None

    best_idx = 0
    best_conf = 0.0
    for idx in range(len(keypoints_data.data)):
        avg_conf = keypoints_data.data[idx][:, 2].mean().item()
        if avg_conf > best_conf:
            best_conf = avg_conf
            best_idx = idx

    return keypoints_data.data[best_idx].cpu().numpy()


def _normalize_feature(value: float, history: list[float], min_range: float):
    if len(history) < 8:
        return None, None, None

    low = float(np.percentile(history, 10))
    high = float(np.percentile(history, 90))
    if (high - low) < min_range:
        return None, low, high

    normalized = float(np.clip((value - low) / (high - low), 0.0, 1.0))
    return normalized, low, high


def _situp_segment_confidence(min_up_score: float, conf_values: list[float]) -> int:
    depth_bonus = max(0.0, 0.30 - min_up_score)
    low_conf = min(conf_values)
    score = 82 + (depth_bonus * 50.0) + (low_conf * 10.0)
    return int(np.clip(score, 80, 99))


def _vertical_jump_segment_confidence(peak_center_lift: float, peak_ankle_lift: float, conf_values: list[float]) -> int:
    low_conf = min(conf_values)
    score = 82 + (peak_center_lift * 12.0) + (peak_ankle_lift * 10.0) + (low_conf * 6.0)
    return int(np.clip(score, 82, 99))


def _shuttle_segment_confidence(travel_ratio: float, conf_values: list[float]) -> int:
    low_conf = min(conf_values)
    score = 80 + (travel_ratio * 12.0) + (low_conf * 8.0)
    return int(np.clip(score, 80, 98))


def _smooth_series(values: list[float]) -> np.ndarray:
    arr = np.array(values, dtype=np.float64)
    if len(arr) < 5:
        return arr
    kernel = np.array([1.0, 2.0, 3.0, 2.0, 1.0], dtype=np.float64)
    kernel /= kernel.sum()
    padded = np.pad(arr, (2, 2), mode="edge")
    return np.convolve(padded, kernel, mode="valid")


def _draw_status_text(
    frame,
    text: str,
    origin: tuple[int, int],
    color: tuple[int, int, int],
    font_scale: float = 0.8,
    thickness: int = 2,
):
    x, y = origin
    font = cv2.FONT_HERSHEY_SIMPLEX
    (text_w, text_h), baseline = cv2.getTextSize(text, font, font_scale, thickness)
    top_left = (max(0, x - 12), max(0, y - text_h - 14))
    bottom_right = (min(frame.shape[1] - 1, x + text_w + 12), min(frame.shape[0] - 1, y + baseline + 10))
    cv2.rectangle(frame, top_left, bottom_right, (18, 18, 18), -1)
    cv2.putText(
        frame,
        text,
        (x, y),
        font,
        font_scale,
        color,
        thickness,
        cv2.LINE_AA,
    )


def _transcode_for_opencv(video_path: Path) -> Path:
    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    transcoded_path = video_path.with_name(f"{video_path.stem}_opencv.mp4")
    cmd = [
        ffmpeg_exe,
        "-y",
        "-i",
        str(video_path),
        "-vf",
        "scale='min(1280,iw)':-2",
        "-c:v",
        "libx264",
        "-preset",
        "veryfast",
        "-pix_fmt",
        "yuv420p",
        "-movflags",
        "+faststart",
        "-an",
        str(transcoded_path),
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    return transcoded_path


def _compute_situp_features(kps):
    left_chain_conf = float(min(kps[KP_L_SHOULDER][2], kps[KP_L_HIP][2], kps[KP_L_KNEE][2], kps[KP_L_ANKLE][2]))
    right_chain_conf = float(min(kps[KP_R_SHOULDER][2], kps[KP_R_HIP][2], kps[KP_R_KNEE][2], kps[KP_R_ANKLE][2]))

    shoulder = _avg_kp(kps, KP_L_SHOULDER, KP_R_SHOULDER)
    hip = _avg_kp(kps, KP_L_HIP, KP_R_HIP)
    knee = _avg_kp(kps, KP_L_KNEE, KP_R_KNEE)
    ankle = _avg_kp(kps, KP_L_ANKLE, KP_R_ANKLE)

    avg_chain_conf = min(
        _kp_conf(kps, KP_L_SHOULDER, KP_R_SHOULDER),
        _kp_conf(kps, KP_L_HIP, KP_R_HIP),
        _kp_conf(kps, KP_L_KNEE, KP_R_KNEE),
        _kp_conf(kps, KP_L_ANKLE, KP_R_ANKLE),
    )

    angle_candidates = []
    conf_values = []
    if left_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        angle_candidates.append(calculate_angle(_point(kps, KP_L_SHOULDER), _point(kps, KP_L_HIP), _point(kps, KP_L_KNEE)))
        conf_values.append(left_chain_conf)
    if right_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        angle_candidates.append(calculate_angle(_point(kps, KP_R_SHOULDER), _point(kps, KP_R_HIP), _point(kps, KP_R_KNEE)))
        conf_values.append(right_chain_conf)
    if not angle_candidates and avg_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        angle_candidates.append(calculate_angle(shoulder, hip, knee))
        conf_values.append(avg_chain_conf)

    if not angle_candidates:
        return None

    torso_length = max(_distance(shoulder, hip), 1.0)
    compression = _distance(shoulder, knee) / torso_length
    extension = _distance(shoulder, ankle) / torso_length
    torso_horizontal = abs(shoulder[0] - hip[0]) / torso_length
    torso_vertical = abs(shoulder[1] - hip[1]) / torso_length

    return {
        "angle": float(sum(angle_candidates) / len(angle_candidates)),
        "compression": float(compression),
        "extension": float(extension),
        "torso_horizontal": float(torso_horizontal),
        "torso_vertical": float(torso_vertical),
        "conf_values": conf_values,
        "shoulder": shoulder,
        "hip": hip,
        "knee": knee,
    }


def _compute_situp_score(
    features,
    angle_history,
    compression_history,
    extension_history,
    torso_horizontal_history,
    torso_vertical_history,
):
    components = {}
    ranges = {}

    def add_component(name, value, history, min_range, invert=False):
        normalized, low, high = _normalize_feature(value, history, min_range=min_range)
        ranges[name] = (low, high)
        if normalized is None:
            return
        components[name] = 1.0 - normalized if invert else normalized

    add_component("angle", features["angle"], angle_history, min_range=18.0)
    add_component("compression", features["compression"], compression_history, min_range=0.12)
    add_component("extension", features["extension"], extension_history, min_range=0.12)
    add_component("torso_horizontal", features["torso_horizontal"], torso_horizontal_history, min_range=0.08)
    add_component("torso_vertical", features["torso_vertical"], torso_vertical_history, min_range=0.08, invert=True)

    if not components:
        return None, ranges

    score = float(sum(components.values()) / len(components))
    return score, ranges


def _compute_vertical_jump_features(kps, frame_h: int):
    shoulder = _avg_kp(kps, KP_L_SHOULDER, KP_R_SHOULDER)
    hip = _avg_kp(kps, KP_L_HIP, KP_R_HIP)
    knee = _avg_kp(kps, KP_L_KNEE, KP_R_KNEE)
    ankle = _avg_kp(kps, KP_L_ANKLE, KP_R_ANKLE)

    left_chain_conf = float(min(kps[KP_L_SHOULDER][2], kps[KP_L_HIP][2], kps[KP_L_KNEE][2], kps[KP_L_ANKLE][2]))
    right_chain_conf = float(min(kps[KP_R_SHOULDER][2], kps[KP_R_HIP][2], kps[KP_R_KNEE][2], kps[KP_R_ANKLE][2]))
    avg_chain_conf = min(
        _kp_conf(kps, KP_L_SHOULDER, KP_R_SHOULDER),
        _kp_conf(kps, KP_L_HIP, KP_R_HIP),
        _kp_conf(kps, KP_L_KNEE, KP_R_KNEE),
        _kp_conf(kps, KP_L_ANKLE, KP_R_ANKLE),
    )
    if max(left_chain_conf, right_chain_conf, avg_chain_conf) < MIN_KEYPOINT_CONFIDENCE:
        return None

    knee_angles = []
    if left_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        knee_angles.append(calculate_angle(_point(kps, KP_L_HIP), _point(kps, KP_L_KNEE), _point(kps, KP_L_ANKLE)))
    if right_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        knee_angles.append(calculate_angle(_point(kps, KP_R_HIP), _point(kps, KP_R_KNEE), _point(kps, KP_R_ANKLE)))
    if not knee_angles:
        knee_angles.append(calculate_angle(hip, knee, ankle))

    hip_angles = []
    if left_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        hip_angles.append(calculate_angle(_point(kps, KP_L_SHOULDER), _point(kps, KP_L_HIP), _point(kps, KP_L_KNEE)))
    if right_chain_conf >= MIN_KEYPOINT_CONFIDENCE:
        hip_angles.append(calculate_angle(_point(kps, KP_R_SHOULDER), _point(kps, KP_R_HIP), _point(kps, KP_R_KNEE)))
    if not hip_angles:
        hip_angles.append(calculate_angle(shoulder, hip, knee))

    center_y = float(((hip[1] * 2.0) + shoulder[1]) / (3.0 * frame_h))
    ankle_y = float(ankle[1] / frame_h)
    body_length = float(max(_distance(shoulder, ankle) / frame_h, 1e-3))

    return {
        "shoulder": shoulder,
        "hip": hip,
        "knee": knee,
        "ankle": ankle,
        "center_y": center_y,
        "ankle_y": ankle_y,
        "body_length": body_length,
        "knee_angle": float(sum(knee_angles) / len(knee_angles)),
        "hip_angle": float(sum(hip_angles) / len(hip_angles)),
        "confidence": float(max(left_chain_conf, right_chain_conf, avg_chain_conf)),
    }


def _compute_shuttle_features(kps, frame_w: int, frame_h: int):
    shoulder = _avg_kp(kps, KP_L_SHOULDER, KP_R_SHOULDER)
    hip = _avg_kp(kps, KP_L_HIP, KP_R_HIP)
    ankle = _avg_kp(kps, KP_L_ANKLE, KP_R_ANKLE)

    avg_chain_conf = min(
        _kp_conf(kps, KP_L_SHOULDER, KP_R_SHOULDER),
        _kp_conf(kps, KP_L_HIP, KP_R_HIP),
        _kp_conf(kps, KP_L_ANKLE, KP_R_ANKLE),
    )
    if avg_chain_conf < MIN_KEYPOINT_CONFIDENCE:
        return None

    center_x = float(((hip[0] * 2.0) + shoulder[0]) / (3.0 * frame_w))
    center_y = float(((hip[1] * 2.0) + shoulder[1]) / (3.0 * frame_h))
    body_length = float(max(_distance(shoulder, ankle) / frame_h, 1e-3))

    return {
        "shoulder": shoulder,
        "hip": hip,
        "ankle": ankle,
        "center_x": center_x,
        "center_y": center_y,
        "body_length": body_length,
        "confidence": float(avg_chain_conf),
    }


def _analyze_shuttle_run(samples: list[dict], shuttle_target: int = 10):
    if len(samples) < 12:
        return 0.0, []

    times = [float(sample["time_ms"]) for sample in samples]
    confs = [float(sample["confidence"]) for sample in samples]
    xs_raw = [float(sample["center_x"]) for sample in samples]
    xs = _smooth_series(xs_raw)

    x_low = float(np.percentile(xs, 5))
    x_high = float(np.percentile(xs, 95))
    travel_range = x_high - x_low
    if travel_range < 0.12:
        return 0.0, []

    left_enter = x_low + (travel_range * 0.12)
    right_enter = x_high - (travel_range * 0.12)
    left_exit = x_low + (travel_range * 0.24)
    right_exit = x_high - (travel_range * 0.24)
    turn_peak_progress_min = 0.82
    reversal_margin_progress = 0.03
    min_shuttle_ms = 450.0
    min_turn_peak_ms = 280.0
    same_side_timeout_ms = 2200.0

    def zone_for(x_value: float) -> str:
        if x_value <= left_enter:
            return "LEFT"
        if x_value >= right_enter:
            return "RIGHT"
        return "MIDDLE"

    last_touch_side = None
    last_touch_time = 0.0
    depart_time = None
    progress_peak = 0.0
    traversal_conf = []
    turn_peak_time = 0.0
    turn_peak_x = 0.0
    segments = []

    for idx, x_value in enumerate(xs):
        current_time_ms = times[idx]
        zone = zone_for(float(x_value))
        conf = confs[idx]

        if last_touch_side is None:
            if zone in {"LEFT", "RIGHT"}:
                last_touch_side = zone
                last_touch_time = current_time_ms
            continue

        if depart_time is None:
            if last_touch_side == "LEFT":
                if x_value >= left_exit:
                    depart_time = current_time_ms
                    traversal_conf = [conf]
                    progress_peak = max(0.0, (x_value - x_low) / max(travel_range, 1e-3))
                    turn_peak_time = current_time_ms
                    turn_peak_x = float(x_value)
                elif zone == "LEFT":
                    last_touch_time = current_time_ms
            else:
                if x_value <= right_exit:
                    depart_time = current_time_ms
                    traversal_conf = [conf]
                    progress_peak = max(0.0, (x_high - x_value) / max(travel_range, 1e-3))
                    turn_peak_time = current_time_ms
                    turn_peak_x = float(x_value)
                elif zone == "RIGHT":
                    last_touch_time = current_time_ms
            continue

        traversal_conf.append(conf)
        if last_touch_side == "LEFT":
            progress_value = max(0.0, (x_value - x_low) / max(travel_range, 1e-3))
            if progress_value >= progress_peak:
                progress_peak = progress_value
                turn_peak_time = current_time_ms
                turn_peak_x = float(x_value)
            returned_same_side = zone == "LEFT"
            reached_opposite = zone == "RIGHT"
            opposite_side = "RIGHT"
            reversed_after_peak = (
                turn_peak_time < current_time_ms
                and ((turn_peak_x - float(x_value)) / max(travel_range, 1e-3)) >= reversal_margin_progress
            )
        else:
            progress_value = max(0.0, (x_high - x_value) / max(travel_range, 1e-3))
            if progress_value >= progress_peak:
                progress_peak = progress_value
                turn_peak_time = current_time_ms
                turn_peak_x = float(x_value)
            returned_same_side = zone == "RIGHT"
            reached_opposite = zone == "LEFT"
            opposite_side = "LEFT"
            reversed_after_peak = (
                turn_peak_time < current_time_ms
                and ((float(x_value) - turn_peak_x) / max(travel_range, 1e-3)) >= reversal_margin_progress
            )

        peak_turn_detected = (
            progress_peak >= turn_peak_progress_min
            and reversed_after_peak
            and (turn_peak_time - depart_time) >= min_turn_peak_ms
            and (turn_peak_time - last_touch_time) >= min_shuttle_ms
        )

        if reached_opposite and (current_time_ms - last_touch_time) >= min_shuttle_ms:
            segments.append(
                {
                    "label": f"Shuttle {len(segments) + 1}",
                    "start_time_ms": int(max(depart_time, 0.0)),
                    "end_time_ms": int(current_time_ms),
                    "confidence": _shuttle_segment_confidence(progress_peak, traversal_conf or [conf]),
                }
            )
            last_touch_side = zone
            last_touch_time = current_time_ms
            depart_time = None
            progress_peak = 0.0
            traversal_conf = []
            turn_peak_time = 0.0
            turn_peak_x = 0.0
        elif peak_turn_detected:
            segments.append(
                {
                    "label": f"Shuttle {len(segments) + 1}",
                    "start_time_ms": int(max(depart_time, 0.0)),
                    "end_time_ms": int(turn_peak_time),
                    "confidence": _shuttle_segment_confidence(progress_peak, traversal_conf or [conf]),
                }
            )
            last_touch_side = opposite_side
            last_touch_time = turn_peak_time
            depart_time = None
            progress_peak = 0.0
            traversal_conf = []
            turn_peak_time = 0.0
            turn_peak_x = 0.0
        elif returned_same_side and (current_time_ms - depart_time) > same_side_timeout_ms:
            depart_time = None
            progress_peak = 0.0
            traversal_conf = []
            turn_peak_time = 0.0
            turn_peak_x = 0.0

    if len(segments) >= shuttle_target:
        value = (segments[shuttle_target - 1]["end_time_ms"] - segments[0]["start_time_ms"]) / 1000.0
        segments = segments[:shuttle_target]
    else:
        value = 0.0

    return float(round(value, 2)), segments


def _shuttle_overlay_state(segments: list[dict], current_time_ms: float, shuttle_target: int = 10):
    shuttle_segments = [
        segment for segment in segments if str(segment.get("label", "")).startswith("Shuttle")
    ]
    completed = sum(1 for segment in shuttle_segments if current_time_ms >= float(segment.get("end_time_ms", 0)))
    active_segment = next(
        (
            segment
            for segment in shuttle_segments
            if float(segment.get("start_time_ms", 0)) <= current_time_ms < float(segment.get("end_time_ms", 0))
        ),
        None,
    )

    if active_segment is not None:
        stage_text = f"{active_segment.get('label', 'Shuttle')} in progress"
    elif not shuttle_segments:
        stage_text = "No shuttle crossings detected"
    elif completed >= shuttle_target:
        stage_text = "Full drill complete"
    elif completed >= len(shuttle_segments):
        stage_text = "Waiting for next shuttle"
    else:
        next_segment = shuttle_segments[completed]
        stage_text = f"Approaching {next_segment.get('label', 'next shuttle')}"

    return completed, active_segment, stage_text


def _render_shuttle_overlay(annotated_path: Path, segments: list[dict], output_fps: float, shuttle_target: int = 10):
    if not annotated_path.exists():
        return None

    cap = cv2.VideoCapture(str(annotated_path))
    if not cap.isOpened():
        print(f"[shuttle_run] Could not reopen annotated video for live overlay: {annotated_path}")
        return None

    render_fps = cap.get(cv2.CAP_PROP_FPS)
    if render_fps == 0 or np.isnan(render_fps):
        render_fps = output_fps if output_fps > 0 else 12.0

    frame_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    overlay_path = annotated_path.with_name(f"{annotated_path.stem}_live.mp4")
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out_writer = cv2.VideoWriter(str(overlay_path), fourcc, render_fps, (frame_w, frame_h))
    if not out_writer.isOpened():
        cap.release()
        print(f"[shuttle_run] Could not create live overlay video: {overlay_path}")
        return None

    frame_idx = 0
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        current_time_ms = (frame_idx / render_fps) * 1000.0
        completed, active_segment, stage_text = _shuttle_overlay_state(
            segments,
            current_time_ms,
            shuttle_target=shuttle_target,
        )
        remaining = max(0, shuttle_target - completed)

        _draw_status_text(
            frame,
            f"Shuttles: {completed}/{shuttle_target}",
            (24, 52),
            (0, 255, 0),
            font_scale=0.95,
            thickness=2,
        )
        _draw_status_text(
            frame,
            f"Stage: {stage_text}",
            (24, 98),
            (255, 255, 255),
            font_scale=0.78,
            thickness=2,
        )

        if active_segment is not None:
            detail_text = f"Live: {active_segment.get('label', 'Shuttle')} crossing"
        elif remaining == 0:
            detail_text = "Result locked from the first 10 completed shuttles"
        elif completed == 0:
            detail_text = "Count starts after the first confirmed boundary-to-boundary traverse"
        else:
            detail_text = f"{remaining} more shuttle(s) needed for a full score"

        _draw_status_text(
            frame,
            detail_text,
            (24, 140),
            (255, 255, 0),
            font_scale=0.72,
            thickness=2,
        )

        out_writer.write(frame)
        frame_idx += 1

    cap.release()
    out_writer.release()
    del cap
    del out_writer
    if overlay_path.exists():
        print(f"[shuttle_run] Added live overlay to {overlay_path.name}")
        return overlay_path
    return None


def process_video(video_path: str, test_type: str):
    model = _get_model()
    video_path_obj = Path(video_path)

    cap = cv2.VideoCapture(str(video_path_obj))
    if not cap.isOpened():
        print(f"[pose_evaluator] OpenCV could not open {video_path_obj}. Attempting ffmpeg transcode...")
        transcoded_path = _transcode_for_opencv(video_path_obj)
        cap = cv2.VideoCapture(str(transcoded_path))
        if not cap.isOpened():
            print(f"[pose_evaluator] Failed to open video even after transcode: {transcoded_path}")
            return 0.0, [], None

    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps == 0 or np.isnan(fps):
        fps = 30.0
    frame_time_ms = 1000.0 / fps
    frame_stride = _frame_stride_for(fps, test_type)

    frame_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    annotated_filename = f"{video_path_obj.stem}_annotated.mp4"
    annotated_path = video_path_obj.parent / annotated_filename

    output_fps = max(1.0, fps / frame_stride)
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out_writer = cv2.VideoWriter(str(annotated_path), fourcc, output_fps, (frame_w, frame_h))

    value = 0.0
    segments = []
    frame_idx = 0

    situp_state = "UNKNOWN"
    rep_start = 0.0
    frames_in_new_state = 0
    debounce_frames = 2
    pending_state = None
    last_rep_end = -10000.0
    min_up_score = 1.0
    min_rep_ms = 550.0
    max_rep_ms = 10000.0
    score_up_threshold = 0.30
    score_down_threshold = 0.70
    required_up_depth = 0.25
    angle_history = []
    compression_history = []
    extension_history = []
    torso_horizontal_history = []
    torso_vertical_history = []
    score_history = []
    current_ranges = None
    situp_calibrated = False

    ankles_y_history = []
    in_air = False
    flight_start = 0.0
    jump_state = "READY"
    jump_prep_start = 0.0
    jump_takeoff = 0.0
    jump_peak_center_lift = 0.0
    jump_peak_ankle_lift = 0.0
    jump_conf_values = []
    vertical_ankle_history = []
    vertical_center_history = []
    vertical_body_length_history = []
    vertical_knee_history = []
    vertical_feature_history = []
    vertical_calibrated = False

    hip_x_history = []
    direction = None
    laps = 0
    lap_start = 0.0
    shuttle_target = 10
    shuttle_state = "TRACKING"
    shuttle_samples = []

    is_passing = False
    pass_start = 0.0
    passes = 0

    print(
        f"[pose_evaluator] Processing {total_frames} frames at {fps:.1f} fps "
        f"(stride={frame_stride}) for test: {test_type}"
    )

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        current_time_ms = (frame_idx / fps) * 1000.0
        annotated_frame = frame.copy()

        results = model(frame, verbose=False, imgsz=INFERENCE_IMAGE_SIZE)
        kps = _extract_keypoints(results)
        if results and results[0].boxes is not None:
            annotated_frame = results[0].plot()

        if kps is not None:
            if test_type == "situps":
                features = _compute_situp_features(kps)
                if features is not None:
                    angle_history.append(features["angle"])
                    compression_history.append(features["compression"])
                    extension_history.append(features["extension"])
                    torso_horizontal_history.append(features["torso_horizontal"])
                    torso_vertical_history.append(features["torso_vertical"])
                    for history in (
                        angle_history,
                        compression_history,
                        extension_history,
                        torso_horizontal_history,
                        torso_vertical_history,
                    ):
                        if len(history) > 240:
                            history.pop(0)

                    signal_score, current_ranges = _compute_situp_score(
                        features,
                        angle_history,
                        compression_history,
                        extension_history,
                        torso_horizontal_history,
                        torso_vertical_history,
                    )

                    if signal_score is not None:
                        score_history.append(signal_score)
                        if len(score_history) > 10:
                            score_history.pop(0)
                        smoothed_score = float(np.mean(score_history[-3:]))

                        if not situp_calibrated and len(angle_history) >= max(12, 24 // frame_stride):
                            angle_range = current_ranges.get("angle")
                            compression_range = current_ranges.get("compression")
                            extension_range = current_ranges.get("extension")
                            horizontal_range = current_ranges.get("torso_horizontal")
                            vertical_range = current_ranges.get("torso_vertical")
                            print(
                                "[situp] Calibrated score ranges: "
                                f"angle={angle_range}, compression={compression_range}, extension={extension_range}, "
                                f"torso_horizontal={horizontal_range}, torso_vertical={vertical_range}"
                            )
                            situp_calibrated = True

                        if situp_state == "UNKNOWN":
                            if smoothed_score >= 0.5:
                                situp_state = "DOWN"
                            else:
                                situp_state = "UP"
                                rep_start = current_time_ms
                                min_up_score = smoothed_score

                        shoulder_px = (int(features["shoulder"][0]), int(features["shoulder"][1]))
                        hip_px = (int(features["hip"][0]), int(features["hip"][1]))
                        knee_px = (int(features["knee"][0]), int(features["knee"][1]))
                        cv2.putText(
                            annotated_frame,
                            f"Hip: {features['angle']:.0f}",
                            (hip_px[0] - 40, hip_px[1] - 20),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            0.7,
                            (0, 255, 255),
                            2,
                        )
                        cv2.putText(
                            annotated_frame,
                            f"Score: {smoothed_score:.2f}",
                            (20, 130),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            0.8,
                            (255, 255, 0),
                            2,
                        )
                        cv2.line(annotated_frame, shoulder_px, hip_px, (0, 255, 255), 2)
                        cv2.line(annotated_frame, hip_px, knee_px, (0, 255, 255), 2)

                        if situp_state == "DOWN" and smoothed_score <= score_up_threshold:
                            if pending_state != "UP":
                                pending_state = "UP"
                                frames_in_new_state = 1
                            else:
                                frames_in_new_state += 1

                            if frames_in_new_state >= debounce_frames:
                                situp_state = "UP"
                                rep_start = current_time_ms - ((debounce_frames - 1) * frame_time_ms * frame_stride)
                                min_up_score = smoothed_score
                                pending_state = None
                                frames_in_new_state = 0

                        elif situp_state == "UP":
                            min_up_score = min(min_up_score, smoothed_score)
                            if smoothed_score >= score_down_threshold:
                                if pending_state != "DOWN":
                                    pending_state = "DOWN"
                                    frames_in_new_state = 1
                                else:
                                    frames_in_new_state += 1

                                if frames_in_new_state >= debounce_frames:
                                    rep_end = current_time_ms
                                    rep_duration = rep_end - rep_start
                                    completed_full_motion = min_up_score <= required_up_depth
                                    if (
                                        completed_full_motion
                                        and min_rep_ms <= rep_duration <= max_rep_ms
                                        and (rep_end - last_rep_end) >= (min_rep_ms * 0.6)
                                    ):
                                        value += 1
                                        segments.append(
                                            {
                                                "label": f"Rep {int(value)}",
                                                "start_time_ms": int(max(rep_start, 0)),
                                                "end_time_ms": int(rep_end),
                                                "confidence": _situp_segment_confidence(
                                                    min_up_score,
                                                    features["conf_values"],
                                                ),
                                            }
                                        )
                                        last_rep_end = rep_end

                                    situp_state = "DOWN"
                                    min_up_score = 1.0
                                    pending_state = None
                                    frames_in_new_state = 0
                            elif pending_state is not None:
                                pending_state = None
                                frames_in_new_state = 0
                        else:
                            pending_state = None
                            frames_in_new_state = 0

                cv2.putText(
                    annotated_frame,
                    f"Sit-ups: {int(value)}",
                    (20, 50),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1.2,
                    (0, 255, 0),
                    3,
                )
                cv2.putText(
                    annotated_frame,
                    f"State: {situp_state}",
                    (20, 90),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    (255, 255, 255),
                    2,
                )

            elif test_type == "vertical_jump":
                features = _compute_vertical_jump_features(kps, frame_h)
                if features is not None:
                    if jump_state != "AIRBORNE":
                        vertical_ankle_history.append(features["ankle_y"])
                        vertical_center_history.append(features["center_y"])
                        vertical_body_length_history.append(features["body_length"])
                        vertical_knee_history.append(features["knee_angle"])
                        vertical_feature_history.append(
                            {
                                "ankle_y": features["ankle_y"],
                                "center_y": features["center_y"],
                                "body_length": features["body_length"],
                                "knee_angle": features["knee_angle"],
                            }
                        )
                        for history in (
                            vertical_ankle_history,
                            vertical_center_history,
                            vertical_body_length_history,
                            vertical_knee_history,
                        ):
                            if len(history) > 120:
                                history.pop(0)
                        if len(vertical_feature_history) > 120:
                            vertical_feature_history.pop(0)

                    if len(vertical_ankle_history) >= 8:
                        body_lengths = np.array([item["body_length"] for item in vertical_feature_history], dtype=np.float64)
                        body_gate = float(np.percentile(body_lengths, 72))
                        standing_frames = [item for item in vertical_feature_history if item["body_length"] >= body_gate]
                        if len(standing_frames) < 5:
                            standing_frames = vertical_feature_history

                        standing_ankles = [item["ankle_y"] for item in standing_frames]
                        standing_centers = [item["center_y"] for item in standing_frames]
                        standing_bodies = [item["body_length"] for item in standing_frames]
                        standing_knees = [item["knee_angle"] for item in standing_frames]

                        baseline_ankle = float(np.percentile(standing_ankles, 80))
                        baseline_center = float(np.percentile(standing_centers, 60))
                        baseline_body = float(max(np.percentile(standing_bodies, 70), 0.18))
                        baseline_knee = float(np.percentile(standing_knees, 70))
                        ankle_lift_ratio = max(0.0, (baseline_ankle - features["ankle_y"]) / baseline_body)
                        center_lift_ratio = max(0.0, (baseline_center - features["center_y"]) / baseline_body)
                        crouch_depth_ratio = max(0.0, (features["center_y"] - baseline_center) / baseline_body)
                        knee_bend_ratio = max(0.0, (baseline_knee - features["knee_angle"]) / 180.0)

                        if not vertical_calibrated and len(vertical_ankle_history) >= max(10, 28 // frame_stride):
                            print(
                                "[vertical_jump] Calibrated baselines: "
                                f"ankle={baseline_ankle:.3f}, center={baseline_center:.3f}, "
                                f"body={baseline_body:.3f}, knee={baseline_knee:.1f}"
                            )
                            vertical_calibrated = True

                        if jump_state == "READY":
                            if ankle_lift_ratio >= 0.10 and center_lift_ratio >= 0.18:
                                jump_state = "AIRBORNE"
                                jump_prep_start = max(0.0, current_time_ms - (frame_time_ms * frame_stride * 2.0))
                                jump_takeoff = max(0.0, current_time_ms - ((frame_time_ms * frame_stride) * 0.5))
                                jump_peak_center_lift = center_lift_ratio
                                jump_peak_ankle_lift = ankle_lift_ratio
                                jump_conf_values = [features["confidence"]]
                            elif crouch_depth_ratio >= 0.10 or (knee_bend_ratio >= 0.10 and features["knee_angle"] <= baseline_knee - 16.0):
                                jump_state = "DIP"
                                jump_prep_start = current_time_ms

                        elif jump_state == "DIP":
                            if (
                                ankle_lift_ratio >= 0.08
                                and center_lift_ratio >= 0.18
                                and features["knee_angle"] >= max(150.0, baseline_knee - 14.0)
                            ):
                                jump_state = "AIRBORNE"
                                jump_takeoff = max(0.0, current_time_ms - ((frame_time_ms * frame_stride) * 0.5))
                                jump_peak_center_lift = center_lift_ratio
                                jump_peak_ankle_lift = ankle_lift_ratio
                                jump_conf_values = [features["confidence"]]
                            elif (current_time_ms - jump_prep_start) > 2500.0 or (crouch_depth_ratio < 0.03 and knee_bend_ratio < 0.03):
                                jump_state = "READY"

                        elif jump_state == "AIRBORNE":
                            in_air = True
                            flight_start = jump_takeoff
                            jump_peak_center_lift = max(jump_peak_center_lift, center_lift_ratio)
                            jump_peak_ankle_lift = max(jump_peak_ankle_lift, ankle_lift_ratio)
                            jump_conf_values.append(features["confidence"])
                            if len(jump_conf_values) > 24:
                                jump_conf_values.pop(0)

                            flight_time_ms = current_time_ms - jump_takeoff
                            landing_ready = ankle_lift_ratio <= 0.03 and flight_time_ms >= 180.0
                            if landing_ready:
                                in_air = False
                                rep_end = current_time_ms
                                flight_time_s = (rep_end - jump_takeoff) / 1000.0
                                h_cm = 122.625 * (flight_time_s ** 2)
                                completed_jump = jump_peak_center_lift >= 0.20 and jump_peak_ankle_lift >= 0.10
                                if completed_jump and 5.0 <= h_cm <= 150.0:
                                    value = max(value, h_cm)
                                    segment_confidence = _vertical_jump_segment_confidence(
                                        jump_peak_center_lift,
                                        jump_peak_ankle_lift,
                                        jump_conf_values or [features["confidence"]],
                                    )
                                    landing_end = rep_end + min(320.0, frame_time_ms * frame_stride * 4.0)
                                    segments = [
                                        {
                                            "label": "Dip",
                                            "start_time_ms": int(max(jump_prep_start, 0.0)),
                                            "end_time_ms": int(max(jump_takeoff, jump_prep_start)),
                                            "confidence": max(82, segment_confidence - 4),
                                        },
                                        {
                                            "label": "Flight",
                                            "start_time_ms": int(jump_takeoff),
                                            "end_time_ms": int(rep_end),
                                            "confidence": segment_confidence,
                                        },
                                        {
                                            "label": "Landing",
                                            "start_time_ms": int(rep_end),
                                            "end_time_ms": int(landing_end),
                                            "confidence": max(80, segment_confidence - 2),
                                        },
                                    ]
                                jump_state = "READY"
                                jump_peak_center_lift = 0.0
                                jump_peak_ankle_lift = 0.0
                                jump_conf_values = []

                        cv2.putText(
                            annotated_frame,
                            f"Jump: {value:.1f} cm",
                            (20, 50),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            1.0,
                            (0, 255, 0),
                            3,
                        )
                        cv2.putText(
                            annotated_frame,
                            f"State: {jump_state}",
                            (20, 90),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            0.8,
                            (255, 255, 255),
                            2,
                        )
                        cv2.putText(
                            annotated_frame,
                            f"Lift: {jump_peak_center_lift:.2f}",
                            (20, 130),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            0.8,
                            (255, 255, 0),
                            2,
                        )

            elif test_type == "shuttle_run":
                features = _compute_shuttle_features(kps, frame_w, frame_h)
                if features is not None:
                    shuttle_samples.append(
                        {
                            "time_ms": current_time_ms,
                            "center_x": features["center_x"],
                            "center_y": features["center_y"],
                            "body_length": features["body_length"],
                            "confidence": features["confidence"],
                        }
                    )

            elif "endurance_run" in test_type:
                conf_s = float(kps[KP_L_SHOULDER][2])
                conf_a = float(kps[KP_L_ANKLE][2])
                if conf_s > MIN_KEYPOINT_CONFIDENCE and conf_a > MIN_KEYPOINT_CONFIDENCE:
                    y_min = kps[KP_L_SHOULDER][1] / frame_h
                    y_max = kps[KP_L_ANKLE][1] / frame_h
                    height_ratio = abs(y_max - y_min)

                    if height_ratio > 0.45 and not is_passing:
                        is_passing = True
                        pass_start = current_time_ms
                    elif height_ratio < 0.35 and is_passing:
                        is_passing = False
                        passes += 1
                        segments.append(
                            {
                                "label": f"Lap {passes}",
                                "start_time_ms": int(pass_start),
                                "end_time_ms": int(current_time_ms),
                                "confidence": 88,
                            }
                        )

                    target_laps = 2 if test_type == "endurance_run_800m" else 4
                    if passes >= target_laps:
                        value = current_time_ms / 1000.0

        out_writer.write(annotated_frame)

        skipped_frames = 0
        while skipped_frames < frame_stride - 1 and cap.grab():
            skipped_frames += 1
        frame_idx += skipped_frames + 1

        if frame_idx % 100 == 0:
            print(f"[pose_evaluator] Processed {frame_idx}/{total_frames} frames...")

    cap.release()
    out_writer.release()
    del cap
    del out_writer
    final_annotated_path = annotated_path

    if test_type == "shuttle_run":
        value, segments = _analyze_shuttle_run(shuttle_samples, shuttle_target=shuttle_target)
        print(f"[shuttle_run] Post-analysis completed shuttles={len(segments)} value={value}")
        try:
            live_overlay_path = _render_shuttle_overlay(
                annotated_path,
                segments,
                output_fps,
                shuttle_target=shuttle_target,
            )
            if live_overlay_path is not None:
                final_annotated_path = live_overlay_path
        except Exception as exc:
            print(f"[shuttle_run] Failed to add live overlay: {exc}")

    final_time = frame_idx / fps
    if "endurance" in test_type and value == 0:
        value = final_time

    annotated_uri = f"/uploads/{final_annotated_path.name}" if final_annotated_path.exists() else None
    print(f"[pose_evaluator] Done! test={test_type} value={value} segments={len(segments)}")
    return float(round(value, 2)), segments, annotated_uri

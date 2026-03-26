import cv2
import math
import numpy as np
import mediapipe as mp
import os

def calculate_angle(a, b, c):
    """
    Calculate the angle between three points (a, b, c) where b is the vertex.
    a, b, c = [x, y]
    """
    a = np.array(a)
    b = np.array(b)
    c = np.array(c)
    
    radians = np.arctan2(c[1]-b[1], c[0]-b[0]) - np.arctan2(a[1]-b[1], a[0]-b[0])
    angle = np.abs(radians*180.0/np.pi)
    
    if angle > 180.0:
        angle = 360 - angle
        
    return angle

def process_video(video_path: str, test_type: str):
    """
    Process a video file with Mediapipe Pose and return:
    - final score/value (e.g. reps, cm, seconds)
    - segments (e.g. [{"label": "Rep 1", "start_time_ms": 100, "end_time_ms": 2000, "confidence": 95}])
    - annotated_filename String
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"Failed to open video: {video_path}")
        return 0.0, [], ""

    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps == 0 or np.isnan(fps):
        fps = 30.0
        
    w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    # Prepare output video
    dir_name = os.path.dirname(video_path)
    base_name = os.path.basename(video_path)
    name, ext = os.path.splitext(base_name)
    annotated_filename = f"{name}_annotated.mp4"
    out_path = os.path.join(dir_name, annotated_filename)
    
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(out_path, fourcc, fps, (w, h))

    mp_pose = mp.solutions.pose
    mp_drawing = mp.solutions.drawing_utils
    mp_drawing_styles = mp.solutions.drawing_styles
    
    pose = mp_pose.Pose(
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
        model_complexity=1 # Use 1 for accuracy, or 0 for speed
    )

    value = 0.0
    segments = []

    # Logic state variables
    current_time_ms = 0.0
    
    # Situps
    state = "DOWN"
    rep_start = 0

    # Vertical Jump
    ankles_y_history = []
    flight_start = 0
    in_air = False

    # Shuttle Run
    hip_x_history = []
    direction = None
    laps = 0
    lap_start = 0

    # Pushups & Squats
    exercise_state = "UP"
    rep_start = 0

    frame_idx = 0
    
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
            
        current_time_ms = (frame_idx / fps) * 1000.0

        # Recolor image to RGB
        image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        image.flags.writeable = False

        # Make detection
        results = pose.process(image)
        
        # Recolor back to BGR for drawing and saving
        image.flags.writeable = True
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        
        if results.pose_landmarks:
            # Draw pose landmarks
            mp_drawing.draw_landmarks(
                image,
                results.pose_landmarks,
                mp_pose.POSE_CONNECTIONS,
                landmark_drawing_spec=mp_drawing_styles.get_default_pose_landmarks_style()
            )
            
            landmarks = results.pose_landmarks.landmark
            
            if test_type == "situps":
                # Get coordinates
                shoulder = [landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].x,
                            landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].y]
                hip = [landmarks[mp_pose.PoseLandmark.LEFT_HIP.value].x,
                       landmarks[mp_pose.PoseLandmark.LEFT_HIP.value].y]
                knee = [landmarks[mp_pose.PoseLandmark.LEFT_KNEE.value].x,
                        landmarks[mp_pose.PoseLandmark.LEFT_KNEE.value].y]
                
                # Calculate angle
                angle = calculate_angle(shoulder, hip, knee)
                
                # Logic transitions
                if angle > 130:
                    if state == "UP":
                        state = "DOWN"
                        value += 1
                        segments.append({
                            "label": f"Rep {int(value)}",
                            "start_time_ms": int(rep_start),
                            "end_time_ms": int(current_time_ms),
                            "confidence": 95
                        })
                elif angle < 85 and state == "DOWN":
                    state = "UP"
                    rep_start = current_time_ms
                    
            elif test_type == "vertical_jump":
                ankle_l = landmarks[mp_pose.PoseLandmark.LEFT_ANKLE.value].y
                ankle_r = landmarks[mp_pose.PoseLandmark.RIGHT_ANKLE.value].y
                avg_ankle_y = (ankle_l + ankle_r) / 2.0
                ankles_y_history.append(avg_ankle_y)
                
                if len(ankles_y_history) > 5:
                    baseline = np.mean(ankles_y_history[-5:-1])
                    if baseline - avg_ankle_y > 0.05 and not in_air: 
                        in_air = True
                        flight_start = current_time_ms
                    elif avg_ankle_y - baseline > 0.02 and in_air: 
                        in_air = False
                        flight_time = (current_time_ms - flight_start) / 1000.0
                        jump_height_m = 0.125 * 9.81 * (flight_time ** 2)
                        h_cm = jump_height_m * 100
                        if h_cm > value and h_cm < 150: 
                            value = h_cm
                            segments.append({
                                "label": "Max Jump",
                                "start_time_ms": int(flight_start),
                                "end_time_ms": int(current_time_ms),
                                "confidence": 90
                            })
                            
            elif test_type == "shuttle_run":
                hip_x = (landmarks[mp_pose.PoseLandmark.LEFT_HIP.value].x + landmarks[mp_pose.PoseLandmark.RIGHT_HIP.value].x) / 2.0
                hip_x_history.append(hip_x)
                
                if len(hip_x_history) > 10:
                    prev_x = hip_x_history[-10]
                    curr_dir = "R" if hip_x > prev_x else "L"
                    if direction is None:
                        direction = curr_dir
                    elif direction != curr_dir and abs(hip_x - prev_x) > 0.1:
                        direction = curr_dir
                        laps += 1
                        segments.append({
                            "label": f"Lap {laps}",
                            "start_time_ms": int(lap_start),
                            "end_time_ms": int(current_time_ms),
                            "confidence": 92
                        })
                        lap_start = current_time_ms
                value = (current_time_ms / 1000.0) if laps >= 10 else 0.0
                    
            elif test_type == "pushups":
                shoulder = [landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].x, landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].y]
                elbow = [landmarks[mp_pose.PoseLandmark.LEFT_ELBOW.value].x, landmarks[mp_pose.PoseLandmark.LEFT_ELBOW.value].y]
                wrist = [landmarks[mp_pose.PoseLandmark.LEFT_WRIST.value].x, landmarks[mp_pose.PoseLandmark.LEFT_WRIST.value].y]
                
                angle = calculate_angle(shoulder, elbow, wrist)
                if angle < 100 and exercise_state == "UP":
                    exercise_state = "DOWN"
                    rep_start = current_time_ms
                elif angle > 150 and exercise_state == "DOWN":
                    exercise_state = "UP"
                    value += 1
                    segments.append({
                        "label": f"Rep {int(value)}",
                        "start_time_ms": int(rep_start),
                        "end_time_ms": int(current_time_ms),
                        "confidence": 92
                    })

            elif test_type == "squats":
                hip = [landmarks[mp_pose.PoseLandmark.LEFT_HIP.value].x, landmarks[mp_pose.PoseLandmark.LEFT_HIP.value].y]
                knee = [landmarks[mp_pose.PoseLandmark.LEFT_KNEE.value].x, landmarks[mp_pose.PoseLandmark.LEFT_KNEE.value].y]
                ankle = [landmarks[mp_pose.PoseLandmark.LEFT_ANKLE.value].x, landmarks[mp_pose.PoseLandmark.LEFT_ANKLE.value].y]
                
                angle = calculate_angle(hip, knee, ankle)
                if angle < 120 and exercise_state == "UP":
                    exercise_state = "DOWN"
                    rep_start = current_time_ms
                elif angle > 160 and exercise_state == "DOWN":
                    exercise_state = "UP"
                    value += 1
                    segments.append({
                        "label": f"Rep {int(value)}",
                        "start_time_ms": int(rep_start),
                        "end_time_ms": int(current_time_ms),
                        "confidence": 92
                    })

        # Draw text overlay regarding reps / score
        cv2.putText(image, f"Value: {round(value, 1)}", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)

        # Write frame to output
        out.write(image)

        frame_idx += 1

    cap.release()
    out.release()
    pose.close()
    
    # Fallback/Time captures if conditions aren't perfectly met in video
    final_time = (frame_idx / fps)
    
    if test_type == "shuttle_run" and laps < 10:
        value = final_time
        
    return float(round(value, 2)), segments, f"/uploads/{annotated_filename}"

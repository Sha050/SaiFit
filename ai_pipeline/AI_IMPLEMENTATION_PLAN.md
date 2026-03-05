# SAI Fit: AI Evaluation Pipeline Plan

This document outlines the detailed strategy and algorithms for implementing on-device AI evaluation using TensorFlow Lite models right on the Android app. Evaluating fitness parameters dynamically from video input presents unique computer vision challenges, handled below.

## Foundational Setup
We will use **TensorFlow Lite (TFLite)** coupled with **Google's MoveNet** (Lightning or Thunder variant) for on-device pose estimation. MoveNet output is an array of 17 $(y, x, \text{confidence})$ keypoints representing human joints.

1. Nose
2. Left eye / 3. Right eye
4. Left ear / 5. Right ear
6. Left shoulder / 7. Right shoulder
8. Left elbow / 9. Right elbow
10. Left wrist / 11. Right wrist
12. Left hip / 13. Right hip
14. Left knee / 15. Right knee
16. Left ankle / 17. Right ankle

---

## 1. Height Measurement
**The Approach**: Manual Input based on user instructions.
- **Implementation**: The Android app will display a clean numeric input field prompting the user to enter their height in centimeters (cm). The AI-based measurement is removed to ensure accuracy without requiring complex object calibration.

## 2. Body Weight
**The Approach**: Manual Input based on user instructions.
- **Implementation**: The user reads their weight from a digital scale and types it directly into a numeric input field in the app. This replaces AI OCR and guarantees exact values without fail.

## 3. Sit Ups (Core)
**The Problem**: We need to count valid repetitions, not just wiggling.
**The Solution**: Tracking the angle of the core via MoveNet.
- **Implementation**:
  - Extract Keypoints: Shoulders (6/7), Hips (12/13), Knees (14/15).
  - State Machine:
    - State 1 (Rest/Down): The shoulder is near the floor. The angle `Knee-Hip-Shoulder` is roughly 150°-180° on the horizontal plane.
    - State 2 (Up/Climax): The user sits up. The angle `Knee-Hip-Shoulder` shifts downward (towards 45°-90°), and the Shoulder `Y` pixel position moves significantly higher.
  - A full valid rep is registered when the state transitions `Down -> Up -> Down`. We add debounce logic (a cooldown of 500ms) to ensure double-counts don't occur.

## 4. Vertical Jumps (Power)
**The Problem**: It's hard to measure a jump directly in pixels because the camera might not be placed uniformly.
**The Solution**: Flight time physics. The amount of time you are not touching the floor perfectly correlates to how high you jumped.
- **Implementation**:
  - Requires 60FPS camera recording (smoothness required).
  - Track Ankle keypoints (16/17).
  - Detect the exact moment $T_1$ the ankles start consistent upward vertical movement without touching the "base" line.
  - Detect the exact moment $T_2$ the ankles return to the base line.
  - Flight time $t = T_2 - T_1$.
  - Use physics formula: $h = \frac{1}{8} g t^2$, where $g = 9.81 m/s^2$. Multiply by 100 for centimeters.

## 5. Shuttle Run (Agility)
**The Problem**: We need to ensure the user actually runs back and forth the full required distance.
**The Solution**: Lateral bounding Tracking.
- **Implementation**:
  - Place camera in the center of the shuttle run track, perpendicular to the track.
  - Track the centroid (center of hips) `X` coordinate.
  - As the athlete runs right, `X` increases. As they hit the line and run left, `X` velocity drops to 0 and becomes negative.
  - The app looks for these zero-crossings (spikes/valleys in a velocity graph) at the far edges of the frame to count laps.

## 6. Endurance Runs (800m / 1600m)
**The Problem**: Hardware arrays (GPS/accelerometers) are not allowed, but tracking a continuous 1600m distance on a single stationary phone camera over a huge area is physically impossible directly.
**The Solution**: Video-Based Loop Counting.
- **Implementation**:
  - The runner must be instructed to run on a looping track or a repetitive straight line (e.g., a 400m Olympic track, or running back and forth on a 50m line).
  - The camera is placed stationary at the start/finish line.
  - We deploy the SSD MobileNet (Object Detection) model to identify a "Person" in frame.
  - When the person passes close to the camera, their bounding box size increases rapidly, passing a defined vertical/horizontal threshold line.
  - This registers as 1 Loop/Pass. 
  - The evaluator tallies passes until the target distance (e.g., 4 laps for 1600m on a 400m track) is achieved, logging the total elapsed time. No GPS or pedometer required.

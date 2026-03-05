from fastapi import APIRouter, File, UploadFile, HTTPException
import aiofiles
import os
import uuid

router = APIRouter()

# Use absolute path relative to this file's location
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Maximum file size: 100 MB
MAX_FILE_SIZE = 100 * 1024 * 1024


@router.post("/")
async def upload_video(file: UploadFile = File(...)):
    """
    Upload a video file for processing.
    Saves to local disk and returns a URI.
    """
    # Validate content type
    content_type = file.content_type or ""
    if not content_type.startswith("video/") and content_type != "application/octet-stream":
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type '{content_type}'. Must be a video (video/mp4, video/webm, etc.) or application/octet-stream.",
        )

    # Determine file extension
    file_ext = ""
    if file.filename:
        file_ext = os.path.splitext(file.filename)[1]
    if not file_ext:
        file_ext = ".mp4"

    unique_filename = f"{uuid.uuid4()}{file_ext}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)

    try:
        # Read and write in chunks to handle large files
        total_size = 0
        async with aiofiles.open(file_path, "wb") as out_file:
            while True:
                chunk = await file.read(1024 * 1024)  # 1 MB chunks
                if not chunk:
                    break
                total_size += len(chunk)
                if total_size > MAX_FILE_SIZE:
                    # Clean up partial file
                    await out_file.close()
                    os.remove(file_path)
                    raise HTTPException(status_code=413, detail="File too large. Maximum 100 MB.")
                await out_file.write(chunk)
    except HTTPException:
        raise
    except Exception as e:
        # Clean up on error
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=f"Failed to upload video: {str(e)}")

    local_uri = f"/uploads/{unique_filename}"

    return {
        "video_uri": local_uri,
        "filename": unique_filename,
        "size_bytes": total_size,
        "status": "success",
    }


@router.get("/list")
async def list_uploads():
    """List all uploaded video files."""
    files = []
    if os.path.exists(UPLOAD_DIR):
        for f in os.listdir(UPLOAD_DIR):
            fpath = os.path.join(UPLOAD_DIR, f)
            if os.path.isfile(fpath):
                files.append({
                    "filename": f,
                    "size_bytes": os.path.getsize(fpath),
                    "uri": f"/uploads/{f}",
                })
    return {"uploads": files, "count": len(files)}

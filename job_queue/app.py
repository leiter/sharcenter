"""
Flask API server for job queue management.
Provides endpoints to submit jobs via JSON.
"""
import json
import logging
import os
import sys
import uuid
from flask import Flask, request, jsonify
from job_queue_manager import get_queue_manager, create_job_from_dict
from job_types import JOB_REGISTRY
from werkzeug.utils import secure_filename

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)

app = Flask(__name__)

# Configure upload folder
UPLOAD_FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024  # 100MB max upload size


# Initialize and start the queue manager
queue_manager = get_queue_manager()
queue_manager.start()

logger.info("Flask application initialized")


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint."""
    stats = queue_manager.get_stats()
    return jsonify({
        "status": "healthy",
        "queue_manager": stats
    }), 200


@app.route('/jobs', methods=['POST'])
def submit_job():
    """
    Submit a new job to the queue.

    Expected JSON format:
    {
        "job_type": "CriticalSystemJob",
        "job_id": "optional-custom-id",
        "data": {
            "operation": "system_restart",
            ... other job-specific fields
        }
    }
    """
    try:
        if not request.is_json:
            return jsonify({
                "error": "Content-Type must be application/json"
            }), 400

        job_data = request.get_json()

        logger.info(f"Received job submission request: {job_data}")

        # Create job from the request data
        job = create_job_from_dict(job_data)

        # Submit to queue
        queue_manager.submit_job(job)

        return jsonify({
            "status": "success",
            "message": "Job submitted successfully",
            "job_id": job.job_id,
            "job_type": job.job_type,
            "priority": job.priority
        }), 202

    except ValueError as e:
        logger.error(f"Invalid job data: {str(e)}")
        return jsonify({
            "error": str(e)
        }), 400

    except Exception as e:
        logger.error(f"Error submitting job: {str(e)}", exc_info=True)
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/jobs/upload', methods=['POST'])
def upload_files():
    """
    Upload files and create a job to process them.

    Expected multipart form data:
    - files: One or more files to upload
    - job_type: Type of job to create (default: BackgroundJob)
    - job_id: Optional custom job ID (auto-generated if not provided)
    - task_data: JSON string with task metadata
    """
    try:
        # Get files from request
        files = request.files.getlist('files')
        if not files or all(f.filename == '' for f in files):
            return jsonify({
                "error": "No files provided"
            }), 400

        # Get job parameters
        job_type = request.form.get('job_type', 'BackgroundJob')
        job_id = request.form.get('job_id', str(uuid.uuid4()))
        task_data_str = request.form.get('task_data', '{}')

        try:
            task_data = json.loads(task_data_str)
        except json.JSONDecodeError:
            task_data = {}

        logger.info(f"Received upload request: job_id={job_id}, job_type={job_type}, files={len(files)}")

        # Create job-specific directory
        job_dir = os.path.join(app.config['UPLOAD_FOLDER'], job_id)
        os.makedirs(job_dir, exist_ok=True)

        # Save files
        saved_files = []
        for file in files:
            if file and file.filename:
                filename = secure_filename(file.filename)
                if not filename:
                    filename = f"file_{len(saved_files)}"
                filepath = os.path.join(job_dir, filename)
                file.save(filepath)
                saved_files.append({
                    "filename": filename,
                    "path": filepath,
                    "size": os.path.getsize(filepath)
                })
                logger.info(f"Saved file: {filename} ({os.path.getsize(filepath)} bytes)")

        # Add saved file info to task data
        task_data['saved_files'] = saved_files

        # Create job
        job_data = {
            "job_type": job_type,
            "job_id": job_id,
            "data": {
                "task_data": task_data,
                "source": "file_upload"
            }
        }

        job = create_job_from_dict(job_data)
        queue_manager.submit_job(job)

        return jsonify({
            "status": "success",
            "message": "Files uploaded and job submitted",
            "job_id": job_id,
            "job_type": job_type,
            "files_count": len(saved_files),
            "files": [f["filename"] for f in saved_files]
        }), 202

    except Exception as e:
        logger.error(f"Error uploading files: {str(e)}", exc_info=True)
        return jsonify({
            "error": "Upload failed",
            "message": str(e)
        }), 500


@app.route('/jobs/types', methods=['GET'])
def get_job_types():
    """Get available job types."""
    return jsonify({
        "job_types": list(JOB_REGISTRY.keys()),
        "priorities": {
            "CriticalSystemJob": 1,
            "HighPriorityJob": 2,
            "NormalJob": 3,
            "LowPriorityJob": 4,
            "BackgroundJob": 5
        }
    }), 200


@app.route('/queue/stats', methods=['GET'])
def get_stats():
    """Get queue statistics."""
    stats = queue_manager.get_stats()
    return jsonify(stats), 200


if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info("Starting Flask server on http://127.0.0.1:5000")
    logger.info("=" * 60)
    logger.info("Available endpoints:")
    logger.info("  POST   /jobs         - Submit a new job")
    logger.info("  POST   /jobs/upload  - Upload files and create job")
    logger.info("  GET    /jobs/types   - Get available job types")
    logger.info("  GET    /queue/stats  - Get queue statistics")
    logger.info("  GET    /health       - Health check")
    logger.info("=" * 60)

    app.run(debug=True, host='127.0.0.1', port=5000, use_reloader=False)

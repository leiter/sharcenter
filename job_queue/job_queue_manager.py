"""
Job Queue Manager with priority-based execution.
Single worker thread processes jobs sequentially based on priority.
"""
import queue
import threading
import logging
import uuid
from typing import Optional
from job_types import Job, JOB_REGISTRY

logger = logging.getLogger(__name__)


class JobQueueManager:
    """Manages a priority-based job queue with a single worker thread."""

    def __init__(self):
        self.job_queue = queue.PriorityQueue()
        self.worker_thread: Optional[threading.Thread] = None
        self.running = False
        self.lock = threading.Lock()
        self.jobs_processed = 0
        self.jobs_failed = 0

        logger.info("=" * 60)
        logger.info("Job Queue Manager initialized")
        logger.info("=" * 60)

    def start(self):
        """Start the worker thread."""
        with self.lock:
            if self.running:
                logger.warning("Worker thread is already running")
                return

            self.running = True
            self.worker_thread = threading.Thread(target=self._worker, daemon=True)
            self.worker_thread.start()

            logger.info("🚀 Worker thread started - Ready to process jobs")

    def stop(self):
        """Stop the worker thread gracefully."""
        with self.lock:
            if not self.running:
                logger.warning("Worker thread is not running")
                return

            logger.info("Stopping worker thread...")
            self.running = False

        if self.worker_thread:
            self.worker_thread.join(timeout=10)
            logger.info("✓ Worker thread stopped")

    def submit_job(self, job: Job):
        """Submit a job to the queue."""
        if not self.running:
            logger.error(f"Cannot submit job {job.job_id} - Queue manager not running")
            raise RuntimeError("Queue manager is not running")

        queue_size = self.job_queue.qsize()
        logger.info("─" * 60)
        logger.info(f"📥 NEW JOB SUBMITTED:")
        logger.info(f"   Job ID: {job.job_id}")
        logger.info(f"   Type: {job.job_type}")
        logger.info(f"   Priority: {job.priority}")
        logger.info(f"   Current queue size: {queue_size}")
        logger.info("─" * 60)

        self.job_queue.put(job)

        logger.info(f"✓ Job {job.job_id} added to queue (new queue size: {queue_size + 1})")

    def _worker(self):
        """Worker thread that processes jobs from the queue."""
        logger.info("Worker thread entering main loop...")

        while self.running:
            try:
                # Wait for a job with timeout to allow checking running flag
                try:
                    job = self.job_queue.get(timeout=1)
                except queue.Empty:
                    continue

                queue_size = self.job_queue.qsize()
                logger.info("=" * 60)
                logger.info(f"▶️  STARTING JOB EXECUTION")
                logger.info(f"   Job ID: {job.job_id}")
                logger.info(f"   Type: {job.job_type}")
                logger.info(f"   Priority: {job.priority}")
                logger.info(f"   Remaining jobs in queue: {queue_size}")
                logger.info("=" * 60)

                # Execute the job
                try:
                    job.execute()
                    self.jobs_processed += 1

                    logger.info("=" * 60)
                    logger.info(f"✅ JOB EXECUTION SUCCESSFUL")
                    logger.info(f"   Job ID: {job.job_id}")
                    logger.info(f"   Total processed: {self.jobs_processed}")
                    logger.info(f"   Total failed: {self.jobs_failed}")
                    logger.info("=" * 60)

                except Exception as e:
                    self.jobs_failed += 1
                    logger.error("=" * 60)
                    logger.error(f"❌ JOB EXECUTION FAILED")
                    logger.error(f"   Job ID: {job.job_id}")
                    logger.error(f"   Error: {str(e)}")
                    logger.error(f"   Total processed: {self.jobs_processed}")
                    logger.error(f"   Total failed: {self.jobs_failed}")
                    logger.error("=" * 60)
                finally:
                    self.job_queue.task_done()

                # Log queue status
                if queue_size > 0:
                    logger.info(f"📊 Queue status: {queue_size} job(s) waiting...")
                else:
                    logger.info("📊 Queue is now empty - Waiting for new jobs...")

            except Exception as e:
                logger.error(f"Worker thread error: {str(e)}", exc_info=True)

        logger.info("Worker thread exiting...")

    def get_stats(self):
        """Get queue statistics."""
        return {
            "running": self.running,
            "queue_size": self.job_queue.qsize(),
            "jobs_processed": self.jobs_processed,
            "jobs_failed": self.jobs_failed,
        }


# Global queue manager instance
_queue_manager = None


def get_queue_manager() -> JobQueueManager:
    """Get or create the global queue manager instance."""
    global _queue_manager
    if _queue_manager is None:
        _queue_manager = JobQueueManager()
    return _queue_manager


def create_job_from_dict(job_data: dict) -> Job:
    """
    Create a job instance from a dictionary.

    Expected format:
    {
        "job_type": "CriticalSystemJob",
        "data": {
            "operation": "system_restart",
            ... other job-specific data
        }
    }
    """
    job_type = job_data.get("job_type")
    if not job_type:
        raise ValueError("Missing 'job_type' in job data")

    job_class = JOB_REGISTRY.get(job_type)
    if not job_class:
        raise ValueError(f"Unknown job type: {job_type}. Available types: {list(JOB_REGISTRY.keys())}")

    job_id = job_data.get("job_id", str(uuid.uuid4()))
    data = job_data.get("data", {})

    logger.info(f"Creating job: type={job_type}, job_id={job_id}")

    return job_class(job_id=job_id, data=data)

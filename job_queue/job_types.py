"""
Job types with different priorities for the queue manager.
"""
import time
import random
import logging
from dataclasses import dataclass
from typing import Dict, Any
from abc import ABC, abstractmethod

# Priority constants (lower number = higher priority)
PRIORITY_CRITICAL = 1
PRIORITY_HIGH = 2
PRIORITY_NORMAL = 3
PRIORITY_LOW = 4
PRIORITY_BACKGROUND = 5

logger = logging.getLogger(__name__)


@dataclass
class Job(ABC):
    """Base class for all jobs."""
    job_id: str
    priority: int
    job_type: str

    def __lt__(self, other):
        """Compare jobs by priority for queue ordering."""
        return self.priority < other.priority

    @abstractmethod
    def execute(self):
        """Execute the job."""
        pass

    def _simulate_work(self, task_name: str):
        """Simulate work with random sleep and logging."""
        sleep_time = random.uniform(3, 6)
        logger.info(f"[{self.job_id}] {task_name} - Starting (estimated {sleep_time:.2f}s)")

        # Log at intervals during sleep
        steps = 4
        step_time = sleep_time / steps
        for i in range(steps):
            time.sleep(step_time)
            progress = ((i + 1) / steps) * 100
            logger.info(f"[{self.job_id}] {task_name} - Progress: {progress:.0f}%")

        logger.info(f"[{self.job_id}] {task_name} - Completed")


@dataclass
class CriticalSystemJob(Job):
    """Critical priority job - executes first."""

    def __init__(self, job_id: str, data: Dict[str, Any] = None):
        super().__init__(job_id, PRIORITY_CRITICAL, "CriticalSystemJob")
        self.data = data or {}

    def execute(self):
        logger.warning(f"[{self.job_id}] ⚠️  CRITICAL JOB STARTED - Priority: {self.priority}")
        logger.info(f"[{self.job_id}] System operation type: {self.data.get('operation', 'unknown')}")

        self._simulate_work("Critical system operation")

        logger.warning(f"[{self.job_id}] ✓ CRITICAL JOB COMPLETED")


@dataclass
class HighPriorityJob(Job):
    """High priority job - important but not critical."""

    def __init__(self, job_id: str, data: Dict[str, Any] = None):
        super().__init__(job_id, PRIORITY_HIGH, "HighPriorityJob")
        self.data = data or {}

    def execute(self):
        logger.info(f"[{self.job_id}] 🔥 HIGH PRIORITY JOB STARTED - Priority: {self.priority}")
        logger.info(f"[{self.job_id}] Task details: {self.data.get('task', 'unknown')}")

        self._simulate_work("High priority task")

        logger.info(f"[{self.job_id}] ✓ HIGH PRIORITY JOB COMPLETED")


@dataclass
class NormalJob(Job):
    """Normal priority job - standard queue processing."""

    def __init__(self, job_id: str, data: Dict[str, Any] = None):
        super().__init__(job_id, PRIORITY_NORMAL, "NormalJob")
        self.data = data or {}

    def execute(self):
        logger.info(f"[{self.job_id}] 📋 NORMAL JOB STARTED - Priority: {self.priority}")
        logger.info(f"[{self.job_id}] Processing: {self.data.get('description', 'standard task')}")

        self._simulate_work("Normal task processing")

        logger.info(f"[{self.job_id}] ✓ NORMAL JOB COMPLETED")


@dataclass
class LowPriorityJob(Job):
    """Low priority job - processes when queue is light."""

    def __init__(self, job_id: str, data: Dict[str, Any] = None):
        super().__init__(job_id, PRIORITY_LOW, "LowPriorityJob")
        self.data = data or {}

    def execute(self):
        logger.info(f"[{self.job_id}] 📝 LOW PRIORITY JOB STARTED - Priority: {self.priority}")
        logger.info(f"[{self.job_id}] Background task: {self.data.get('task_name', 'cleanup')}")

        self._simulate_work("Low priority task")

        logger.info(f"[{self.job_id}] ✓ LOW PRIORITY JOB COMPLETED")


@dataclass
class BackgroundJob(Job):
    """Background job - lowest priority, runs when nothing else is queued."""

    def __init__(self, job_id: str, data: Dict[str, Any] = None):
        super().__init__(job_id, PRIORITY_BACKGROUND, "BackgroundJob")
        self.data = data or {}

    def execute(self):
        logger.info(f"[{self.job_id}] 🌙 BACKGROUND JOB STARTED - Priority: {self.priority}")

        task_data = self.data.get('task_data', {})
        if task_data:
            task_type = task_data.get('type', 'Unknown')
            logger.info(f"[{self.job_id}] Task type: {task_type}")
            self._process_by_type(task_type, task_data)
        else:
            # Fallback to legacy behavior
            logger.info(f"[{self.job_id}] Maintenance task: {self.data.get('maintenance_type', 'general')}")
            self._simulate_work("Background maintenance")

        logger.info(f"[{self.job_id}] ✓ BACKGROUND JOB COMPLETED")

    def _process_by_type(self, task_type: str, task_data: dict):
        """Parse and process based on task type."""
        match task_type:
            case "ShareLinksTask":
                links = task_data.get('links', [])
                logger.info(f"[{self.job_id}] ShareLinksTask: {len(links)} links")
                for link in links:
                    logger.info(f"[{self.job_id}]   - {link}")
                self._simulate_work("Processing shared links")

            case "ProcessVideoTask":
                video_id = task_data.get('videoId')
                quality = task_data.get('quality', 'default')
                logger.info(f"[{self.job_id}] ProcessVideoTask: {video_id} @ {quality}")
                self._simulate_work("Processing video")

            case "UploadFilesTask":
                saved_files = task_data.get('saved_files', [])
                file_names = task_data.get('fileNames', [])
                mime_types = task_data.get('mimeTypes', [])
                total_size = task_data.get('totalSize', 0)

                logger.info(f"[{self.job_id}] UploadFilesTask: {len(saved_files)} files uploaded")
                logger.info(f"[{self.job_id}]   File names: {file_names}")
                logger.info(f"[{self.job_id}]   MIME types: {mime_types}")
                logger.info(f"[{self.job_id}]   Total size: {total_size} bytes")

                for file_info in saved_files:
                    filename = file_info.get('filename', 'unknown')
                    filepath = file_info.get('path', '')
                    size = file_info.get('size', 0)
                    logger.info(f"[{self.job_id}]   - {filename}: {size} bytes @ {filepath}")

                self._simulate_work("Processing uploaded files")

            case _:
                logger.warning(f"[{self.job_id}] Unknown task type: {task_type}")
                logger.info(f"[{self.job_id}] Raw data: {task_data}")
                self._simulate_work("Processing unknown task")


# Job type registry for deserialization
JOB_REGISTRY = {
    "CriticalSystemJob": CriticalSystemJob,
    "HighPriorityJob": HighPriorityJob,
    "NormalJob": NormalJob,
    "LowPriorityJob": LowPriorityJob,
    "BackgroundJob": BackgroundJob,
}

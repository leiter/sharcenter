# Job Queue Manager

A Python-based priority job queue manager with a Flask API for job submission. Features sequential job processing with priority-based ordering.

## Features

- **Priority-based Queue**: Jobs are executed based on priority, not submission order
- **5 Job Types**: Different job types with predefined priorities
- **Sequential Processing**: Single worker thread processes jobs one at a time
- **Extensive Logging**: Detailed logs showing queue operations and job execution
- **REST API**: Submit jobs via HTTP POST with JSON
- **Observable Execution**: Each job logs progress and sleeps 3-6 seconds

## Job Types and Priorities

| Job Type | Priority | Use Case |
|----------|----------|----------|
| CriticalSystemJob | 1 | Critical system operations that must execute first |
| HighPriorityJob | 2 | Important tasks requiring quick execution |
| NormalJob | 3 | Standard queue processing |
| LowPriorityJob | 4 | Low-priority tasks that can wait |
| BackgroundJob | 5 | Lowest priority maintenance tasks |

## Installation

1. Install dependencies (from the main server directory):
```bash
pip install -r requirements.txt
```

## Usage

### 1. Start the Server

Navigate to the job_queue folder and run:
```bash
cd job_queue
python app.py
```

The server will start on `http://127.0.0.1:5000`

### 2. Submit Jobs

#### Using the Example Script (Recommended)

From the job_queue folder:
```bash
python submit_jobs_example.py
```

Or using the bash test script:
```bash
./test_queue.sh
```

This interactive script offers two demos:
1. **Priority ordering**: Submits jobs in reverse order to show priority-based execution
2. **Dynamic queue**: Submits jobs at different times to show queue reordering

#### Using curl

Submit a critical job:
```bash
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "CriticalSystemJob",
    "data": {
      "operation": "system_restart"
    }
  }'
```

Submit a normal job:
```bash
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "NormalJob",
    "data": {
      "description": "process_data"
    }
  }'
```

#### Using Python requests

```python
import requests

response = requests.post('http://127.0.0.1:5000/jobs', json={
    "job_type": "HighPriorityJob",
    "data": {
        "task": "generate_report"
    }
})

print(response.json())
```

## API Endpoints

### POST /jobs
Submit a new job to the queue.

**Request Body:**
```json
{
  "job_type": "CriticalSystemJob",
  "job_id": "optional-custom-id",
  "data": {
    "operation": "system_restart",
    "custom_field": "value"
  }
}
```

**Response (202 Accepted):**
```json
{
  "status": "success",
  "message": "Job submitted successfully",
  "job_id": "generated-uuid",
  "job_type": "CriticalSystemJob",
  "priority": 1
}
```

### GET /jobs/types
Get available job types and their priorities.

**Response:**
```json
{
  "job_types": ["CriticalSystemJob", "HighPriorityJob", ...],
  "priorities": {
    "CriticalSystemJob": 1,
    "HighPriorityJob": 2,
    ...
  }
}
```

### GET /queue/stats
Get queue statistics.

**Response:**
```json
{
  "running": true,
  "queue_size": 3,
  "jobs_processed": 15,
  "jobs_failed": 0
}
```

### GET /health
Health check endpoint.

## Example: Priority Queue in Action

When you submit jobs in this order:
1. BackgroundJob (priority 5)
2. NormalJob (priority 3)
3. CriticalSystemJob (priority 1)

They will execute in this order:
1. CriticalSystemJob (priority 1) ← Executes first!
2. NormalJob (priority 3)
3. BackgroundJob (priority 5) ← Executes last

## Log Output Example

```
================================================================
▶️  STARTING JOB EXECUTION
   Job ID: job-critical-1
   Type: CriticalSystemJob
   Priority: 1
   Remaining jobs in queue: 2
================================================================
[job-critical-1] ⚠️  CRITICAL JOB STARTED - Priority: 1
[job-critical-1] System operation type: security_patch
[job-critical-1] Critical system operation - Starting (estimated 4.52s)
[job-critical-1] Critical system operation - Progress: 25%
[job-critical-1] Critical system operation - Progress: 50%
[job-critical-1] Critical system operation - Progress: 75%
[job-critical-1] Critical system operation - Progress: 100%
[job-critical-1] Critical system operation - Completed
[job-critical-1] ✓ CRITICAL JOB COMPLETED
================================================================
✅ JOB EXECUTION SUCCESSFUL
   Job ID: job-critical-1
   Total processed: 1
   Total failed: 0
================================================================
```

## Architecture

- **job_types.py**: Defines 5 job classes with priority constants
- **job_queue_manager.py**: Priority queue implementation with worker thread
- **app.py**: Flask REST API server
- **submit_jobs_example.py**: Example client with demo scenarios

## File Structure

```
server/
├── requirements.txt            # Python dependencies
└── job_queue/                  # Job queue system folder
    ├── app.py                      # Flask API server
    ├── job_queue_manager.py        # Queue manager with worker thread
    ├── job_types.py                # Job type definitions
    ├── submit_jobs_example.py      # Example job submission script
    ├── test_queue.sh               # Quick test script
    └── README_QUEUE.md             # This file
```

## Future Enhancements (Not Implemented)

- Job persistence (database/file storage)
- Multiple worker threads
- Queue monitoring dashboard
- Job status tracking
- Job cancellation
- Scheduled jobs
- Job retry mechanism

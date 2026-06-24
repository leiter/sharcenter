"""
Example script to submit jobs to the job queue via the Flask API.
Demonstrates priority-based queue ordering.
"""
import requests
import time
import json

API_URL = "http://127.0.0.1:5000"


def submit_job(job_type, data=None, job_id=None):
    """Submit a job to the queue."""
    payload = {
        "job_type": job_type,
        "data": data or {}
    }
    if job_id:
        payload["job_id"] = job_id

    response = requests.post(f"{API_URL}/jobs", json=payload)
    print(f"\n{'='*60}")
    print(f"Submitted: {job_type}")
    if response.status_code == 202:
        result = response.json()
        print(f"✓ Success - Job ID: {result['job_id']}, Priority: {result['priority']}")
    else:
        print(f"✗ Failed - {response.status_code}: {response.text}")
    print(f"{'='*60}")
    return response


def get_stats():
    """Get queue statistics."""
    response = requests.get(f"{API_URL}/queue/stats")
    if response.status_code == 200:
        return response.json()
    return None


def get_job_types():
    """Get available job types."""
    response = requests.get(f"{API_URL}/jobs/types")
    if response.status_code == 200:
        return response.json()
    return None


def demo_priority_ordering():
    """Demonstrate priority ordering by submitting jobs in reverse priority order."""
    print("\n" + "="*60)
    print("DEMO: Priority-Based Queue Ordering")
    print("="*60)
    print("Submitting jobs in REVERSE priority order...")
    print("Watch how they execute in PRIORITY order!")
    print("="*60)

    # Submit jobs in reverse priority order (lowest to highest)
    # They should execute in priority order: Critical -> High -> Normal -> Low -> Background

    submit_job("BackgroundJob", {
        "maintenance_type": "cache_cleanup",
        "info": "Submitted FIRST, should execute LAST"
    }, "job-background-1")

    time.sleep(0.5)

    submit_job("LowPriorityJob", {
        "task_name": "log_rotation",
        "info": "Submitted SECOND, should execute FOURTH"
    }, "job-low-1")

    time.sleep(0.5)

    submit_job("NormalJob", {
        "description": "data_processing",
        "info": "Submitted THIRD, should execute THIRD"
    }, "job-normal-1")

    time.sleep(0.5)

    submit_job("HighPriorityJob", {
        "task": "user_report_generation",
        "info": "Submitted FOURTH, should execute SECOND"
    }, "job-high-1")

    time.sleep(0.5)

    submit_job("CriticalSystemJob", {
        "operation": "security_patch",
        "info": "Submitted LAST, should execute FIRST!"
    }, "job-critical-1")

    print("\n" + "="*60)
    print("All jobs submitted!")
    print("Check the server logs to see priority-based execution")
    print("="*60)


def demo_mixed_submissions():
    """Submit jobs at different times to show queue reordering."""
    print("\n" + "="*60)
    print("DEMO: Dynamic Priority Queue")
    print("="*60)
    print("Submitting jobs with delays to show dynamic reordering...")
    print("="*60)

    # Submit some low priority jobs
    submit_job("NormalJob", {"description": "task_1"}, "dynamic-normal-1")
    submit_job("LowPriorityJob", {"task_name": "task_2"}, "dynamic-low-1")

    print("\nWaiting 8 seconds...")
    time.sleep(8)

    # Submit high priority job that should jump the queue
    print("\nNow submitting HIGH PRIORITY job - should execute next!")
    submit_job("HighPriorityJob", {
        "task": "urgent_task",
        "info": "Submitted late but should jump ahead!"
    }, "dynamic-high-1")

    # Submit more low priority
    submit_job("BackgroundJob", {"maintenance_type": "task_3"}, "dynamic-bg-1")

    print("\nWaiting 8 seconds...")
    time.sleep(8)

    # Submit critical job
    print("\nNow submitting CRITICAL job - should execute immediately next!")
    submit_job("CriticalSystemJob", {
        "operation": "emergency_fix",
        "info": "Critical job submitted - takes precedence!"
    }, "dynamic-critical-1")

    print("\n" + "="*60)
    print("Dynamic submission complete!")
    print("="*60)


if __name__ == "__main__":
    print("\n" + "="*60)
    print("Job Queue API - Example Client")
    print("="*60)

    # Check server health
    try:
        response = requests.get(f"{API_URL}/health")
        if response.status_code != 200:
            print("Error: Server is not responding properly")
            exit(1)
        print("✓ Server is healthy")
    except requests.exceptions.ConnectionError:
        print("Error: Cannot connect to server. Is it running?")
        print("Start the server with: python app.py")
        exit(1)

    # Show available job types
    job_types_info = get_job_types()
    if job_types_info:
        print("\nAvailable job types:")
        for job_type, priority in job_types_info['priorities'].items():
            print(f"  - {job_type} (Priority: {priority})")

    # Ask user which demo to run
    print("\n" + "="*60)
    print("Select demo:")
    print("  1. Priority ordering (submit in reverse, execute in priority order)")
    print("  2. Dynamic queue (submit jobs at different times)")
    print("  3. Both demos")
    print("="*60)

    choice = input("Enter choice (1/2/3): ").strip()

    if choice == "1":
        demo_priority_ordering()
    elif choice == "2":
        demo_mixed_submissions()
    elif choice == "3":
        demo_priority_ordering()
        time.sleep(2)
        print("\n\n")
        demo_mixed_submissions()
    else:
        print("Invalid choice. Running priority ordering demo...")
        demo_priority_ordering()

    # Show final stats
    time.sleep(2)
    print("\n" + "="*60)
    print("Final Queue Statistics:")
    stats = get_stats()
    if stats:
        print(json.dumps(stats, indent=2))
    print("="*60)

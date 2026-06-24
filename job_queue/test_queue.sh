#!/bin/bash
# Quick test script for the job queue manager
# Make sure the server is running (python app.py) before executing this script

echo "=================================================="
echo "Job Queue Manager - Quick Test Script"
echo "=================================================="

# Check if server is running
echo -e "\n1. Checking server health..."
curl -s http://127.0.0.1:5000/health | python3 -m json.tool

# Get available job types
echo -e "\n\n2. Getting available job types..."
curl -s http://127.0.0.1:5000/jobs/types | python3 -m json.tool

# Submit jobs in reverse priority order to demonstrate priority queue
echo -e "\n\n3. Submitting jobs in REVERSE priority order..."
echo "   (They should execute in PRIORITY order)"

echo -e "\n   Submitting BackgroundJob (priority 5)..."
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "BackgroundJob",
    "job_id": "test-bg-1",
    "data": {
      "maintenance_type": "cleanup"
    }
  }' | python3 -m json.tool

sleep 1

echo -e "\n   Submitting LowPriorityJob (priority 4)..."
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "LowPriorityJob",
    "job_id": "test-low-1",
    "data": {
      "task_name": "log_rotation"
    }
  }' | python3 -m json.tool

sleep 1

echo -e "\n   Submitting NormalJob (priority 3)..."
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "NormalJob",
    "job_id": "test-normal-1",
    "data": {
      "description": "data_processing"
    }
  }' | python3 -m json.tool

sleep 1

echo -e "\n   Submitting HighPriorityJob (priority 2)..."
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "HighPriorityJob",
    "job_id": "test-high-1",
    "data": {
      "task": "report_generation"
    }
  }' | python3 -m json.tool

sleep 1

echo -e "\n   Submitting CriticalSystemJob (priority 1)..."
curl -X POST http://127.0.0.1:5000/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "job_type": "CriticalSystemJob",
    "job_id": "test-critical-1",
    "data": {
      "operation": "security_update"
    }
  }' | python3 -m json.tool

# Check queue stats
echo -e "\n\n4. Checking queue statistics..."
curl -s http://127.0.0.1:5000/queue/stats | python3 -m json.tool

echo -e "\n\n=================================================="
echo "Jobs submitted! Check the server logs to see"
echo "priority-based execution order."
echo "=================================================="
echo -e "\nExpected execution order:"
echo "  1. CriticalSystemJob (submitted LAST)"
echo "  2. HighPriorityJob"
echo "  3. NormalJob"
echo "  4. LowPriorityJob"
echo "  5. BackgroundJob (submitted FIRST)"
echo "=================================================="

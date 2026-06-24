# Port Forwarding to Virtual Machine

Options for forwarding requests from the Flask server to a VM.

## Option 1: Bind Flask to all interfaces

```python
# In app.py, change:
app.run(host='0.0.0.0', port=5000)
```

Then VM connects to host IP (e.g., `192.168.x.x:5000`)

## Option 2: SSH port forwarding

```bash
# From VM, forward local port to host's Flask server
ssh -L 5000:localhost:5000 user@host_ip
```

## Option 3: VirtualBox/VMware port forwarding

- In VM settings → Network → Port Forwarding
- Host Port: 5000 → Guest Port: 5000

## Option 4: iptables (if VM is on same host)

```bash
# Forward host:5000 to VM:5000
iptables -t nat -A PREROUTING -p tcp --dport 5000 -j DNAT --to-destination VM_IP:5000
```

## Android Emulator

The emulator uses `10.0.2.2` to reach host localhost. If Flask runs on `127.0.0.1:5000`, the emulator can access it at `10.0.2.2:5000`.

This is already configured in `app/src/main/res/xml/network_security_config.xml`:
```xml
<domain includeSubdomains="false">10.0.2.2</domain>
```

And in `JobQueueRepository.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:5000"
```

## Running on VM Guest as a Service

The flow:
```
Android App → Host (port forward) → VM Guest (Flask service)
```

### 1. Copy job_queue to VM

```bash
scp -r job_queue/ user@vm_ip:/path/to/destination/
```

### 2. Install dependencies on VM

```bash
pip install flask
```

### 3. Create systemd service

```ini
# /etc/systemd/system/job_queue.service
[Unit]
Description=Job Queue Flask Server
After=network.target

[Service]
User=your_user
WorkingDirectory=/path/to/job_queue
ExecStart=/usr/bin/python3 app.py
Restart=always

[Install]
WantedBy=multi-user.target
```

### 4. Enable and start service

```bash
sudo systemctl enable job_queue
sudo systemctl start job_queue
sudo systemctl status job_queue  # verify running
```

### 5. View logs

```bash
sudo journalctl -u job_queue -f
```

The host just forwards traffic - all processing happens on the guest VM.

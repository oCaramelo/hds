import os
import json
import sys
import signal
import subprocess

# Terminal Emulator used to spawn the processes
terminal = "kitty" 

# Blockchain node configuration file name
server_configs = [
    "server_default_config.json",
    "server_drop_config.json",
    "server_invalid_signature_config.json",
    "server_wrong_block_config.json",
    "server_delay_config.json"
]

client_configs = [
    "client_default_config.json"
]

server_config = server_configs[0]
client_config = client_configs[0]

server_config_path = f"service/src/main/java/tecnico/resources/{server_config}"
client_config_path = f"client/src/main/java/tecnico/resources/{client_config}"

# Check if configuration files exist
if not os.path.exists(server_config_path):
    print(f"Error: Server config file not found at {server_config_path}")
    sys.exit(1)

if not os.path.exists(client_config_path):
    print(f"Error: Client config file not found at {client_config_path}")
    sys.exit(1)

def quit_handler(*args):
    print("\nShutting down all processes...")
    os.system(f"pkill -i {terminal}")
    sys.exit()

# Compile classes
os.system("mvn clean install")

# Spawn blockchain nodes
print("Starting blockchain nodes...")
with open(server_config_path) as f:
    data = json.load(f)
    for key in data:
        pid = os.fork()
        if pid == 0:
            command = [
                terminal, "--title", f"Node {key['id']}", "sh", "-c",
                f"cd service; mvn exec:java -Dexec.args='{key['id']} src/main/java/tecnico/resources/{server_config} ../{client_config_path}'; sleep 500"
            ]

            subprocess.run(command)
            sys.exit()

# Spawn blockchain clients
print("Starting blockchain clients...")
with open(client_config_path) as f:
    data = json.load(f)
    for key in data:
        pid = os.fork()
        if pid == 0:
          
            command = [
                terminal, "--title", f"Client {key['id']}", "sh", "-c",
                f"cd client; mvn exec:java -Dexec.args='{key['id']} ../{server_config_path} src/main/java/tecnico/resources/{client_config}'; sleep 500"
            ]
            
            subprocess.run(command)
            sys.exit()

# Handle SIGINT (Ctrl+C)
signal.signal(signal.SIGINT, quit_handler)

# Main loop
print("Type 'quit' to exit.")
while True:
    command = input(">> ").strip().lower()
    if command == "quit":
        quit_handler()
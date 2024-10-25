#include <iostream>
#include <cstring>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <cstdio>
#include <unistd.h>
#include <thread>
#include <string>
#include <atomic>
#include <fstream>
#include <fcntl.h>

// using namespace std;

const int PORT = 2680;
const int MAX_BUFFER_SIZE = 4096;
std::atomic<bool> server_running(true);
// bool server_running = true;
// std::atomic<int> server_fd(0);

struct clientInfo {
	sockaddr_in *client_addr;
	socklen_t *client_socklen;
	int client_socket;

	clientInfo(sockaddr_in* ca, socklen_t *cl, int cs) {
		client_addr = ca;
		client_socklen = cl;
		client_socket = cs;
	}
	~clientInfo() {
		delete client_addr;
		delete client_socklen;
	}
};

void thread_handle_client(clientInfo *client_info) {
	std::cerr << "Received request: address = " << client_info->client_addr << ", socket = " << client_info->client_socket << std::endl;
	char server_buffer[MAX_BUFFER_SIZE];
	memset(server_buffer, 0, sizeof(server_buffer));

	int bytes_received = read(client_info->client_socket, server_buffer, MAX_BUFFER_SIZE);
	if(bytes_received <= 0) {
		std::cerr << "Error: Received error bytes from " << client_info->client_addr << std::endl;
		close(client_info->client_socket);
		delete client_info;
		return;
	}
	std::string filename = std::string(server_buffer, bytes_received);
	std::fstream file_read;
	file_read.open(filename, std::ios::in | std::ios::binary);
	if(!file_read.is_open()) {
		std::cerr << "Error opening file " << filename << std::endl;
		std::string error_info = "Error opening file";
		send(client_info->client_socket, error_info.c_str(), error_info.size(), 0);
		close(client_info->client_socket);
		delete client_info;
		return;
	}

	while(!file_read.eof()) {
		memset(server_buffer, 0, sizeof(server_buffer));
		file_read.read(server_buffer, MAX_BUFFER_SIZE);
		int bytes_to_send = file_read.gcount();
		send(client_info->client_socket, server_buffer, bytes_to_send, 0);
	}
	std::cerr << "File [" << filename << "] sent successfully" << std::endl;
	file_read.close();
	close(client_info->client_socket);
	delete client_info;
}

void thread_server() {
        int server_fd;
	struct sockaddr_in server_addr;
	int addrlen = sizeof(server_addr);

	server_fd = socket(AF_INET, SOCK_STREAM, 0);
	if(server_fd == -1) {
		perror("socket creation failed");
		server_running = false;
		return;
	}
	std::cerr << "Server socket = " << server_fd << std::endl;

	int opt = 1;
	if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) == -1) {
		std::cerr << "Failed to set socket option" << std::endl;
		close(server_fd);
		return;
	}

	// int server_flags = fcntl(server_fd, F_GETFL, 0);
	// fcntl(server_fd, F_SETFL, server_flags | O_NONBLOCK);

	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = INADDR_ANY;
	server_addr.sin_port = htons(PORT);
	int bind_status = bind(server_fd, (struct sockaddr *)&server_addr, (socklen_t)sizeof(server_addr));
	if(bind_status == -1) {
		perror("bind failed");
		close(server_fd);
		server_running = false;
		return;
	}

	int listen_status = listen(server_fd, 5);
	if(listen_status == -1) {
		perror("listen failed");
		close(server_fd);
		server_running = false;
		return;
	}

	printf("Server is listening on port %d\n", PORT);

	// fd_set readfds;
	// timeval timeout;
	// timeout.tv_sec = 3;
	// timeout.tv_usec = 0;

	while(server_running) {
		// FD_ZERO(&readfds);
		// FD_SET(server_fd, &readfds);
		// int activity = select(server_fd + 1, &readfds, NULL, NULL, &timeout);
		// if(activity < 0 && errno != EINTR) {
		// 	std::cerr << "Select error" << std::endl;
		// }
		// else if(activity > 0) {
		// // if(FD_ISSET(server_fd, &readfds)) {
		sockaddr_in * client_addr = new sockaddr_in;
		socklen_t *client_socklen = new socklen_t;
		int new_socket = accept(server_fd, (sockaddr *)client_addr, client_socklen);

		if(new_socket < 0) {
			if(errno != EWOULDBLOCK && errno != EAGAIN) {
				std::cerr << "Error accepting new socket" << std::endl;
				std::cerr << "Accept failed: " << strerror(errno) << std::endl;
			}
			delete client_addr;
			delete client_socklen;
			continue;
		}
		clientInfo *client_info = new clientInfo (client_addr, client_socklen, new_socket);
		std::thread client_handler(thread_handle_client, client_info);
		client_handler.detach();
		// }
	}
	close(server_fd);
	std::cerr << "Server exited main server thread." << std::endl;
}

void thread_command_handle() {
	std::string command;
	while(server_running) {
		std::getline(std::cin, command);
		if(command == "exit") {
			std::cerr << "Server exiting ..." << std::endl;
			server_running = false;
			break;
		}
		else {
			std::cerr << "Unknown command: " << command << std::endl;
		}
	}
}

int main() {
	std::thread server_main(thread_server);
	std::thread command_handle(thread_command_handle);
	server_main.join();
	command_handle.join();
	return 0;
}
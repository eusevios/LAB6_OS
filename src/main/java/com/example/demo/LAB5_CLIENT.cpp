#include <iostream>
#include <winsock2.h>
#include <windows.h>
#include <thread>
#include <string>
#include <random>
HANDLE semaphores[] = {
    CreateSemaphoreW(NULL, 1, 1, L"SAMNITE"),
    CreateSemaphoreW(NULL, 1, 1, L"THRACIAN")
};

HANDLE battleEvent;

int main()
{
    char buffer[1024];
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 1), &wsaData)) {
        std::cerr << "Error! Failed to link library.";
        system("pause");
    };

    SOCKET clientSocket;
    SOCKADDR_IN serverAddr;
    char input;

    serverAddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(1111);

    clientSocket = socket(AF_INET, SOCK_STREAM, 0);

    if (connect(clientSocket, (SOCKADDR*)&serverAddr, sizeof(serverAddr))) {
        std::cerr << "Error! Failed to connect server";
        system("pause");
    }

    battleEvent = OpenEventW(EVENT_ALL_ACCESS, FALSE, L"BattleEvent");


    std::cout << "YOU ARE IN COLOSSEUM." << std::endl;

    recv(clientSocket, buffer, sizeof(buffer), 0);
    std::string recvType(buffer);
    int type = std::stoi(recvType);


    recv(clientSocket, buffer, sizeof(buffer), 0);
    std::string recvNum(buffer);
    int num = std::stoi(recvNum);


    if (!semaphores[type]) std::cout << "PIZDA";


    std::string str = "PREPARE FOR FIGHT, ";
    str += type ? "THRACIAN!" : "SAMNITE!";
    std::cout << str << std::endl;

    WaitForSingleObject(semaphores[type], INFINITE);


    send(clientSocket, recvType.substr(0,1).c_str(), 1, 0);
    
    send(clientSocket, recvNum.c_str(), recvNum.length(), 0);

    WaitForSingleObject(battleEvent, INFINITE); 

    std::cout << "THE BATTLE HAS BEGUN, FIGHT WORTHY!" << std::endl;

    int your_health = 50;
    int enemy_health = 50;
    int your_damage = 0;
    int enemy_damage = 0;
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(1, 3);
    while (your_health > 0 && enemy_health > 0) {
        your_damage = dis(gen);
        your_health -= enemy_damage;
        enemy_health -= your_damage;
        std::string str1 = std::to_string(your_damage);
        send(clientSocket, str1.c_str(), str1.length(), 0);
        if (your_health <= 0 || enemy_health <=0) break;
    }
    
    memset(buffer, 0, sizeof(buffer));
    recv(clientSocket, buffer, sizeof(buffer), 0);

    std::cout << buffer << std::endl;

    ReleaseSemaphore(semaphores[type], 1, NULL);

    system("pause");
}
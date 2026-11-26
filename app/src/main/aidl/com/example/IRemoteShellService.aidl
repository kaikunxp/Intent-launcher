package com.example;

interface IRemoteShellService {
    void destroy() = 16777114;
    String runPrivilegedCommand(in String[] args) = 1;
}

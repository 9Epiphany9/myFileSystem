# 📀 OS File System Simulator (操作系统文件管理模拟器)

  

> 一个基于 Java Swing 的图形化文件系统模拟器。它通过软件模拟了磁盘硬件、FAT 显式链接分配、目录树结构以及核心的文件读写操作。

## 📖 项目简介

本项目旨在模拟操作系统底层的文件管理机制。系统不依赖操作系统原生的文件系统 API（如 `File` 类的高级操作），而是通过操作一个二进制文件（`vdisk_storage.dat`）来模拟物理磁盘的磁头读写。

项目采用了 **UI 与内核分离** 的双层架构设计，实现了从物理扇区读写到上层图形界面的完整链路。

## ✨ 核心功能

  * **🖥️ 图形化界面**：基于 Swing 开发的交互界面，支持右键菜单、面包屑导航、实时状态栏。
  * **💾 虚拟磁盘驱动**：模拟 128 块 × 64 字节的物理存储空间，支持扇区级的字节流读写。
  * **🔗 FAT 显式链接**：实现了基于 FAT 表的磁盘空间分配算法，支持跨盘块的大文件存储（自动处理链表跳转）。
  * **📂 多级目录管理**：支持目录的创建、嵌套与导航（类似于 Windows 资源管理器）。
  * **📝 文件读写**：支持创建文件、追加写入数据、全量读取内容。
  * **🔒 权限与并发控制**：
      * 支持修改文件属性（普通/只读）。
      * **系统打开文件表**：防止对正在使用的文件进行删除或修改操作。

## 🏗️ 系统架构

本项目采用 **Layered Architecture (分层架构)**：

```
graph TD
    User[用户交互] --> UI[ui_layer (界面层)]
    UI --> Kernel[sys_core.FileSystemKernel (内核层)]
    Kernel --> OpenTable[SystemOpenTable (状态表)]
    Kernel --> FAT[FAT Manager (空间分配)]
    Kernel --> VDisk[sys_core.VirtualDisk (驱动层)]
    VDisk --> File[(vdisk_storage.dat)]
```

### 核心类说明

| 包 (Package) | 类名 (Class) | 作用描述 |
| :--- | :--- | :--- |
| **sys\_core** | `VirtualDisk` | **虚拟驱动**：处理底层 `RandomAccessFile` 读写，解决 Java 有符号字节越界问题。 |
| **sys\_core** | `FCB` | **元数据**：定义 8 字节的目录项结构 (文件名/属性/起始块/长度)。 |
| **sys\_core** | `FileSystemKernel` | **内核**：整个系统的大脑，负责 FAT 计算、目录解析、IO 流拼接。 |
| **sys\_core** | `SystemOpenTable` | **状态管理**：维护文件句柄，实现多文件并发控制。 |
| **ui\_layer** | `MainWindow` | **主窗口**：负责图标渲染（自适应缩放）、视图刷新。 |
| **ui\_layer** | `FileOperationDialog` | **交互层**：封装右键菜单、新建弹窗、写入框逻辑。 |

## 🚀 快速开始

### 环境要求

  * JDK 1.8 或更高版本
  * IntelliJ IDEA / Eclipse (推荐)

### 运行步骤

1.  克隆或下载本项目。
2.  在 IDE 中打开项目，确保源代码目录 (`src`) 设置正确。
3.  确保 `src/image` 目录下包含资源图片 (`file.png`, `catalogue.png`)，若无图片系统将自动使用色块兜底。
4.  运行 `src/ui_layer/MainWindow.java` 中的 `main` 方法。
5.  系统启动后，会在项目根目录自动生成 `vdisk_storage.dat`（虚拟磁盘文件）。


## 🛠️ 技术细节 (亮点)

1.  **物理寻址修正**：
    在实现 `VirtualDisk` 时，解决了 Java `byte` (-128\~127) 在转换为物理地址时的 **Negative seek offset** 错误，通过位运算 `& 0xFF` 确保了无符号整数的正确处理。

2.  **紧凑型存储**：
    设计了极致紧凑的 **8字节 FCB** 结构，最大化利用了 64 字节的盘块空间。

3.  **动态渲染引擎**：
    UI 层实现了智能图标渲染，能够根据 FCB 属性自动适配图标，并处理了图片缩放与文字排版的冲突。

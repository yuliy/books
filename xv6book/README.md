# xv6: a simple, Unix-like teaching operating system
by Russ Cox, Frans Kaashoek, Robert Morris

## Foreword

This is a draft text intended for a class on operating systems in MIT. It explains the main concepts of operating system by studying an example kernel, named **xv6**. xv6 is modeled on Dennis Ritchie's and Ken Thompson's Unix Version 6 (v6). xv6 loosely follows the structure and style of v6, but is implemented in ANSI C for a multi-core RISC-V.

This text should be read along with the source code for xv6, an approach inspired by John Lions' Commentary on UNIX 6th Edition.


## Useful links
[Official site of the book](https://pdos.csail.mit.edu/6.828/2021/xv6.html)

GitHub repositories:
  * [mit-pdos/xv6-riscv](https://github.com/mit-pdos/xv6-riscv)
    <br>Contains xv6 OS source code (implementation for RISC-V multiprocessor using ANSI C).
  * [mit-pdos/xv6-riscv-book](https://github.com/mit-pdos/xv6-riscv-book)
    <br>Contains source code for build xv6 book.

Built versions of books:
  * [xv6_book.pdf](./books/xv6_book.pdf)
  * [riscv-privileged-v1.9.pdf](./books/riscv-privileged-v1.9.pdf)


## My reading notes
[Chapter 1. Operating system interfaces](./chapter01/README.md)

Chapter 2. Operating system organization

Chapter 3. Page tables

Chapter 4. Traps and system calls

Chapter 5. Interrupts and device drivers

Chapter 6. Locking

Chapter 7. Scheduling

Chapter 8. File system

Chapter 9. Concurrency revisited


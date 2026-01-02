[<<<](../README.md)

# Chapter 2. Building C# Applications
В книге описывается разработка на C# в двух бесплатных средах
  * Visual Studio Comminity Edition - она доступна только для Windows
  * Visual Studio Code - эту IDE я уже знаю, только использую гл обр для разработки на Python и C++.


## Installing .NET 6
Всё необходимое можно скачать отсюда: [dot.net](dot.net)

Конкретно на [этой странице](https://dotnet.microsoft.com/en-us/download/dotnet/6.0) я нашёл ссылки для скачивания всего необходимого именно для версии .NET 6.0
  * ASP.NET Core Runtime 6.0.33
  * .NET Desktop Runtime 6.0.33
  * .NET Runtime 6.0.33
  * SDK 6.0.425 - включает в себя всё выше перечисленное

Нас здесь интересует .NET SDK. Скачал версию для своей ОС - Arm64. На момент написания этого текста всю работу выполнял на MacBook Pro 16-inch 2021 с процессором M1 Max.

### Confirming the .NET 6 Install

После установки в консоли будет доступен вызов программы `dotnet`. Одновременно могут установлены разные версии. Вот таким способом можно посмотреть, что уже установлено:
```bash
> dotnet --list-runtimes
Microsoft.AspNetCore.App 6.0.16 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
Microsoft.AspNetCore.App 6.0.33 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
Microsoft.AspNetCore.App 7.0.8 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
Microsoft.AspNetCore.App 8.0.2 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
Microsoft.NETCore.App 6.0.16 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
Microsoft.NETCore.App 6.0.33 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
Microsoft.NETCore.App 7.0.8 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
Microsoft.NETCore.App 8.0.2 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]

> dotnet --list-sdks
6.0.408 [/usr/local/share/dotnet/sdk]
6.0.425 [/usr/local/share/dotnet/sdk]
7.0.305 [/usr/local/share/dotnet/sdk]
8.0.201 [/usr/local/share/dotnet/sdk]
```

Так можно посмотреть текущую дефолтную версию:
```bash
> dotnet --version
8.0.201
```

А так более расширенную информацию:
```bash
> dotnet --info
.NET SDK:
 Version:           8.0.201
 Commit:            4c2d78f037
 Workload version:  8.0.200-manifests.3097af8b

Runtime Environment:
 OS Name:     Mac OS X
 OS Version:  13.4
 OS Platform: Darwin
 RID:         osx-arm64
 Base Path:   /usr/local/share/dotnet/sdk/8.0.201/

.NET workloads installed:
There are no installed workloads to display.

Host:
  Version:      8.0.2
  Architecture: arm64
  Commit:       1381d5ebd2

.NET SDKs installed:
  6.0.408 [/usr/local/share/dotnet/sdk]
  6.0.425 [/usr/local/share/dotnet/sdk]
  7.0.305 [/usr/local/share/dotnet/sdk]
  8.0.201 [/usr/local/share/dotnet/sdk]

.NET runtimes installed:
  Microsoft.AspNetCore.App 6.0.16 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
  Microsoft.AspNetCore.App 6.0.33 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
  Microsoft.AspNetCore.App 7.0.8 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
  Microsoft.AspNetCore.App 8.0.2 [/usr/local/share/dotnet/shared/Microsoft.AspNetCore.App]
  Microsoft.NETCore.App 6.0.16 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
  Microsoft.NETCore.App 6.0.33 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
  Microsoft.NETCore.App 7.0.8 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]
  Microsoft.NETCore.App 8.0.2 [/usr/local/share/dotnet/shared/Microsoft.NETCore.App]

Other architectures found:
  None

Environment variables:
  Not set

global.json file:
  Not found

Learn more:
  https://aka.ms/dotnet/info

Download .NET:
  https://aka.ms/dotnet/download
```

### Checking For Updates
Проверить актуальность установленных версий можно командой:
```bash
> dotnet sdk check
.NET SDKs:
Version      Status
---------------------------------------------------
6.0.408      .NET 6.0 is going out of support soon.
6.0.425      .NET 6.0 is going out of support soon.
7.0.305      .NET 7.0 is out of support.
8.0.201      Patch 8.0.206 is available.

Try out the newest .NET SDK features with .NET 9.0.100-rc.1.24452.12.

.NET Runtimes:
Name                          Version      Status
---------------------------------------------------------------------------------
Microsoft.AspNetCore.App      6.0.16       .NET 6.0 is going out of support soon.
Microsoft.NETCore.App         6.0.16       .NET 6.0 is going out of support soon.
Microsoft.AspNetCore.App      6.0.33       .NET 6.0 is going out of support soon.
Microsoft.NETCore.App         6.0.33       .NET 6.0 is going out of support soon.
Microsoft.AspNetCore.App      7.0.8        .NET 7.0 is out of support.
Microsoft.NETCore.App         7.0.8        .NET 7.0 is out of support.
Microsoft.AspNetCore.App      8.0.2        Patch 8.0.8 is available.
Microsoft.NETCore.App         8.0.2        Patch 8.0.8 is available.


The latest versions of .NET can be installed from https://aka.ms/dotnet-core-download. For more information about .NET lifecycles, see https://aka.ms/dotnet-core-support.
```

### Use an Earlier Version of the .NET (Core) SDK
Выше было сказано, что `dotnet --version` возвращает "дефолтную" версию. Это либо самая свежая установленная версия, либо версия, указанная в файле `global.json`. Чтобы создать этот файл и переопределить дефолтную версию, необходимо выполнить команду:
```bash
> dotnet --version
8.0.201

> dotnet new globaljson --sdk-version 6.0.425
The template "global.json file" was created successfully.

> dotnet --version
6.0.425

> cat ./global.json
{
  "sdk": {
    "version": "6.0.425"
  }
}
```

Файл `global.json` при этом создаётся в текущей директории и действует, пока мы находимся в этой директории или в любой её поддиректории.


## Building .NET Core Applications with Visual Studio
В книге пишут, что платная и бесплатная версия этой IDE есть и для мака, и для винды. Но я на сайте для мака что-то не нашел. В любом случае я буду пользоваться Visual Studio Code.

## Building .NET Core Applications with Visual Studio Code
Устанавливается всё очень просто:
  1. Скачиваем и устанавливаем VS Code отсюда: https://code.visualstudio.com/download
  2. Также устаналиваем официальный плагин C# от Microsoft - `C# Dev Kit`.

Для работы со всеми проектами в этом репозитории я создал workspace-file [csharp_10_troelsen.code-workspace](../csharp_10_troelsen.code-workspace). Под каждую программу/библиотеку соответственно буду заводить отдельный проект внутри этого воркспейса.

Проекты следует создавать с помощью CLI-утилиты `dotnet`. Сначала создадим .NET solution `MySln` в поддиректории `my_sln`:
```bash
dotnet new sln -n MySln -o my_sln
```

Вообще, создавать solution не обязательно. Делаем это для общности. Далее создаём внутри этого солюшна проект:
```bash
dotnet new console -lang c# -n MySln -o ./my_sln/SimpleCSharpConsoleApp -f net6.0

# !!! Здесь я ошибся и назвал проект MySln
# T.е. я хотел выполнить такую команду:
dotnet new console -lang c# -n SimpleCSharpConsoleApp -o ./my_sln/SimpleCSharpConsoleApp -f net6.0
```

Добавляем проект в солюшн:
```bash
dotnet sln ./my_sln/MySln.sln add ./my_sln/SimpleCSharpConsoleApp
```

### Restoring Packages, Building and Running Program
В .NET есть свой менеджер пакетов - NuGet. Все зависимости для солюшна можно установить такой командой (из директории, где лежит солюшн):
```bash
dotnet restore
```

Для сборки всех проектов солюшна (также из директории, где солюшн):
```bash
dotnet build
```

Команда сборки также установит недостающие зависимости, если это необходимо.

Эти команды аффектят все проекты солюшна. Можно также их выполнить в директории конкретного проекта. Тогда они аффектят только этот проект.

Для запуска собранного приложения (из директории проекта):
```bash
dotnet run
```

## Finding the .NET Core and C# Documentation
Всю документацию по .NET Core и языку C# можно найти на официальном сайте Microsoft здесь
  * [C# language documentation](https://learn.microsoft.com/en-us/dotnet/csharp/)
  * [.NET fundamentals documentation](https://learn.microsoft.com/en-us/dotnet/fundamentals/)

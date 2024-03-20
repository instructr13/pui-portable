# PUI Portable

Processing UI library and Arduino communication library.
Written by Kotlin, needs to build to jvm file to work with Processing.

## Packages

This project contains these packages:

- [PUI](./pui) (dev.wycey.mido.pui) - UI Library with components and state management by
  signal.
  Based on Flutter, BSD 3-Clause "New" or "Revised" License, (c) 2014 The Flutter Authors.
- [Fraiselait](./fraiselait) (dev.wycey.mido.fraiselait) ([Arduino](./arduino/fraiselait)) - Processing - Raspbeery Pi
  Pico (Arduino) communication library with channel recv/send and state locking.
- [Leinwand](./leinwand) (dev.wycey.mido.leinwand) - Simple drawing library with layers and multiple
  brushes support.
- Example [main source](./src/main/kotlin/Main.kt) - Example application with all libraries except
  Fraiselait.

## Building

To build this project, you need to have JDK 17 or later installed.

```shell
./gradlew assemble
```

All built JARs will be stored to `build/libs`.

## Running example application

You don't need to have Processing installed, just run this command and run the built jar file:

```shell
./gradlew pui-portable:run
java -jar build/libs/pui-portable-1.0.0.jar
```

## License

This project is licensed under the GPL-2.0 License **except** the PUI package, which is licensed under
the MIT License - see the following files for details:

- [LICENSE](./LICENSE) - GPL-2.0 License
- [pui/LICENSE](./pui/LICENSE) - MIT License

Reason: Fraiselait uses `processing.serial` package, which
is [licensed under the GPL-2.0 License](https://github.com/benfry/processing4/blob/main/LICENSE.md).
Leinwand depends on Fraiselait, so it's also licensed under the GPL-2.0 License.

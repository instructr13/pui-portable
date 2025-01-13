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

> [!IMPORTANT]
> You need to extract the Processing serial library from the Processing IDE in order to build the project, named as
> `org.processing.serial-4.3.jar` and put it in the `libs` directory.

```shell
./gradlew assemble
```

## Running example application

You don't need to have Processing installed, just run this command and run the built jar file:

```shell
./gradlew run
```

## License

This project is licensed under the GPL-2.0 License **except** the PUI package and Fraiselait Arduino, which are licensed
under the MIT License - see the following files for details:

- [LICENSE](./LICENSE) - GPL-2.0 License
- [pui/LICENSE](./pui/LICENSE) - MIT License
- [arduino/fraiselait/LICENSE](./arduino/fraiselait/LICENSE) - MIT License

Reason: Fraiselait Processing uses `processing.serial` package, which
is [licensed under the GPL-2.0 License](https://github.com/benfry/processing4/blob/main/LICENSE.md).
Leinwand depends on Fraiselait, so it's also licensed under the GPL-2.0 License.

# HorizontalNumberPicker

An horizontal Number picker for android.

## Setup

Import YearPicker, YearPickerAdapter and Utils in your android project to use the numberPicker.

 You can custom them as you want.

## Usage

```xml
<com.your.package.YearPicker
        android:id="@+id/yearpicker"

        android:layout_width="100dp"
        android:layout_height="100dp" />
```

```kotlin
yearPicker.minValue = 10
yearPicker.maxValue = 80
yearPicker.defaultValue = 30
yearPicker.onValueChange = { value -> }
```

## Look

![](HorizontalNumberPicker.gif)

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[GPL-3.0 License](https://www.gnu.org/licenses/gpl-3.0.fr.html)

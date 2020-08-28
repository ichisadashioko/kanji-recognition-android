# Kanji Handwriting Input on Android with TensorFlow Lite

![Imgur](https://i.imgur.com/DHxXd0F.png)

[Demo video](https://youtu.be/NzCLFQ-zo3k)

## Requirements

- If you use Keras Convolution layer as input, `data_format` must be `channels_last`. At the time of this writing (2019-07-24), Tensorflow Lite will throw error when you try to convert CNN model with `data_format` is `channels_first` to `tflite` model.

## Minimal instructions

1. Convert your model to `tflite`.

- Example with Keras model.

```python
model = tf.keras.models.load_model('model.h5')

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite = converter.convert()

tflite_filename = 'model.tflite'
open(tflite_filename, 'wb').write(tflite)
```

2. Create an android project.

3. Add 'tensorflow-lite' to `app/build.gradle` dependencies.

- **Important:** 
    - Add dependencies to the `build.gradle` in the `app` folder, not the `build.gradle` in the root project.
    - You will also need to install [Android NDK](https://developer.android.com/ndk)

- Example `app\build.gradle`

```
...
dependencies {
    ...
    // Tensorflow Lite library
    implementation 'org.tensorflow:tensorflow-lite:0.0.0-nightly'
}
...
```

4. Put your `tflite` model in the `assets` folder. It should locate at `app/src/main/assets`.

5. Tell Gradle not to compress our `tflite` model.

- Add `noCompress 'tflite'` to `app\build.gradle`.

- Example `app\build.gradle`

```
...
android {
    ...
    aaptOptions {
        noCompress 'tflite'
    }
}
...
```

6. Create an `Interpreter` in `Java` or `Kotlin`

After you add `tensorflow-lite` to project's dependencies and sync Gradle, you should able to `import org.tensorflow.lite.Interpreter`.

The `Keras model` equivalent in `TFLite` is an `Interpreter`.

To create and `Interpreter` you will need the `TFLite` model (`MappedByteBuffer`) and a `Interpreter.Options` object.

In order to run inference (or `model.predict` in Keras) you will need to pre-allocated a `ByteBuffer` for the input and a `float[]` (depend on your model) for the output. The input `ByteBuffer` will need to be fill with your data before calling `Interpreter.run(input, output)`.

## References

- Tensorflow official [example](https://github.com/tensorflow/examples) - too complicated for a simple Android app that perform image classification task. However, they do help with my implementation. You just need to find the right file name to read.
- Tensorflow Lite [documentation](https://www.tensorflow.org/lite/guide/inference).

## Телеграм Бот "Cyber-Anna"("Это что за птица?")
[![Build Status](https://travis-ci.com/sbaldin/cyberAnnaBot.svg?branch=master)](https://travis-ci.com/sbaldin/cyberAnnaBot)

Орнитологический телеграмм бот, разрабатывался для фана в чате с друзьями, позволяет распознавать 59 классов птиц России по фото.
Бот использует сверточную нейронную сеть [darknet](https://pjreddie.com/darknet/) на базе библиотеки [dl4j](https://deeplearning4j.org/) для 
классификации входной фотографии. 

Для более точной классификации птиц на входном изображении выделяются объекты с помощью CNN [TinyYOLO](https://arxiv.org/pdf/1612.08242.pdf) из стандартного набора [dl4j](https://deeplearning4j.org/).

Датасет из изображений птиц был собран на основе [100-bird-species](https://www.kaggle.com/gpiosenka/100-bird-species)
(птицы эндемики для США были удалены из датасета, несколько птиц эндемиков для России добавлены).

## Подготовка к запуску 

 Для функционирования бота необходимо получить token от [BotFather](ttps://t.me/BotFather), и прописать его в  `application-config.yaml`:

```yaml
bot:
  name: bot_name
  locale: RU
  token:  TELEGRAM_BOT_API_TOKEN

cnn:
  modelFileName: "model_russian_6x6_kernel_darknet_93.bin"
  cnnInputLayerSize:
    width: 224
    height: 244
    channels: 3
```


## Запуск

При использовании gradle есть нюансы: из-за того что dl4j тянет за собой очень много зависимостей, использование shadow
плагина и специфичных для него shadowJar и shadowRun задач занимает больше времени, а размер jar-файла составляет ~1gb.

```shell
./gradlew -DappConfig="/path/to/application-config.yaml" -Xmx256m -Dorg.bytedeco.javacpp.maxbytes=1100m -Dorg.bytedeco.javacpp.maxphysicalbytes=1200m run
```

## Развертывание

TODO: Предполагается развертывание на raspberry pi4.

### Сборка внутри Docker

Возможна сборка внутри докер контейнера:

```shell
docker run -p 9010:9010 -u gradle -v "$PWD":/home/gradle/project -w /home/gradle/project gradle-6.9 gradle buildDockerRelease --stacktrace --info  --no-daemon \
-Djava.rmi.server.hostname=0.0.0.0 \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.rmi.port=9010 \
-Dcom.sun.management.jmxremote.local.only=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false


```

## Развитие

Добавить подпроект

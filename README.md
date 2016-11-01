# Cloudbox-Android-RX
This library enables you to get your remote files hosted on Cloudbox seamlessly using RXJava.
If you prefer to use the Callbacks way you can use this library (https://github.com/duriana/Cloudbox-Android)

[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=102)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


# Setting up.

1-Import the stable version of the module (Gradle/Maven):
[![](https://jitpack.io/v/duriana/Cloudbox-Android-RX.svg)](https://jitpack.io/#duriana/Cloudbox-Android-RX)



2-if you have extended the class Application.

Add this to <manifest> tag at the top of your manifest file:
```xml
xmlns:tools="http://schemas.android.com/tools" 
```

Add these lines to your <application> tag:
```xml
tools:replace="name"
tools:node:"merge"
```

3-Add the following to the project root build grade:
```groovy
dependencies {
classpath 'com.github.dcendents:android-maven-gradle-plugin:1.4.1'
classpath 'me.tatarka:gradle-retrolambda:2.5.0’
}
```

#How to use

1-In your Application class or any activity you define the single Object CloudBox like this.
```java
CloudBox cloudBox = CloudBox.getInstance(context, YOUR_CLOUDBOX_DOMAIN)
          .setMetaPrefix(CUSTOM_PREFIX);
<!-- where YOUR_CLOUDBOX_DOMAIN = "https://cloudbox.domain.com"
      CUSTOM_PREFIX = "/GBCloudBoxResourcesMeta/" -->
2- Use that object to sync files on background and get observable with the result of the process.
Observable<CloudBox.RESULT> o = cloudBox.getFileFromServerRX(this, "currenciesRates", ".json");
o.observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<CloudBox.RESULT>() {
            @Override
            public void onCompleted() {
            //COMPLETION ACTION
            }

            @Override
            public void onError(Throwable e) {
            //REPORT EXCEPTION.
            }

            @Override
            public void onNext(CloudBox.RESULT result) {
                //SUCCESS ACTION
                if(result == CloudBox.RESULT.FETCHED)
                    //INDICATES THAT NEW VERSION OF THE FILE HAS BEEN DOWNLOADED.
                else if(result == CloudBox.RESULT.UP_TO_DATE)
                    //INDICATES THAT YOU ALREADY HAVE THE NEWEST
        });
```

The code above will Sync the file called 'currenciesRates' by calling the meta URL:
https://cloudbox.domain.com/GBCloudBoxResourcesMeta/currenciesRates.json
then downloads the file if there is a newer version of it.


If you need to block some action until you sync multiple files together or waiting for some other API call of yours
you can use .zip operator to join multiple observables together.
```java
Observable<CloudBox.RESULT> o = cloudBox.getFileFromServerRX(this, "currenciesRates", ".json");
Observable<CloudBox.RESULT> o2 =  cloudBox.getFileFromServerRX(this, "privacyPolicy", ".json");

o2.zipWith(o, new Func2<CloudBox.Result, CloudBox.Result, ArrayList<CloudBox.Result>>() {
            @Override
            public ArrayList<CloudBox.Result> call(CloudBox.Result result1, CloudBox.Result result2) {
                ArrayList<CloudBox.Result> res = new ArrayList<>();
                res.add(result1);
                res.add(result2);
                return res;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ArrayList<CloudBox.Result>>() {
                   ...... 
                });
```

3-Now if you want to get the content of the file you simply type:
```java 
String privacyPolicyString = cloudbox.getFileAsString(MainActivity.this, "privacyPolicy", ".json");
```

4-Keep a fallback version with same name in the assets to use in case of offline use.

Use ```CloudBox.setLogEnabled(true)``` to keep track of the whole process.

Thanks.

rm android.keystore android.keystore.old

keytool -genkey -v -keystore android.keystore -alias android -keyalg RSA -keysize 2048 -validity 10000

keytool -importkeystore -srckeystore android.keystore -destkeystore android.keystore -deststoretype pkcs12
# ScBluetoothSdkDemo_Android

## Add SDK reference
    1.In the first way, copy the SDK aar file to the app/libs/ directory of the project, then configure gradle
        android {
            repositories {
                flatDir {
                dirs 'libs'
                }
            }
        }

        dependencies {
            implementation(name: 'scbluetoothlib-release-v1.0.0-alpha1', ext: 'aar')
        }


    2.The second way, to copy the ScBluetoothLib module configuration of Demo to the project, and then add implementation project(':ScBluetoothLib') to gradle dependencies
## Init before using SDK
    ScBluetoothClient.getInstance().init(getContext());


# Marco Polo
## Augmented Reality Project Wintersemester 2017 / 2018
by Manuel Reich & Arne Schlüter

## Intro
Marco Polo is an augmented reality app, that enables people to find themselves over an audio track that they emit from their position. The name is a nod to the hide and seek game in which one person tries to find the other one by following their voice.

The project consists of an Android application and a web server that can be found in [another repository](https://github.com/ar-marco-polo/marco-polo-server). After starting a game a devices' location is continuously tracked over GPS and sent to the servers, which forwards it to other players that have joined the game via websockets.

## Notes on GPS

Our tests have shown that with mid- to high-end Android devices the average accuracy in places with few obstacles (e.g. public parks) is in the range of &lt; 10m to 20m. This is more than sufficient to locate another person. In other scenarios such as streets with buildings of four or five floors on both sides the average accuracy was around 30m. Inside a building the accuracy can easily get higher, which leads to the game quality being very degraded up until the point of it being unplayable.

We evaluated multiple other tracking technologies, amongst others the [find framework](https://github.com/schollz/find) or Bluetooth Beacons for indoor positioning. Unfortunately none matched up with our goal of bringing the game to as many devices in as many situations as possible.

## How we calculate sound
On modern Android devices we use the fused `Sensor.TYPE_ROTATION_VECTOR` see: https://developer.android.com/guide/topics/sensors/sensors_motion.html to get the device orientation.
On older devices we use `Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_MAGNETIC_FIELD` to get the two compass diretion the device is pointing to.
From this we get a normalized vector in a 3D coordinate system pointing in the device direction.
In fact we use two vectors with a small offset to calculate different volumes for each stereo channel (= ear).

With the position of the device and the position we got from the other player we can calculate the distance to the other
player and a vector pointing in its direction.
Then we can then calulate the angles between the vector pointing to the other player and the vectors of the device.
The lower the angle the higher is the volume representing the other player.

![img](https://github.com/ar-marco-polo/marco-polo-android-app/blob/master/screenshots/Screenshot%202018-01-31%20at%2016.16.33.png)

On the Image you can see the device vectors on pink, the vector of the other player in yellow and the coordinate system in green, red and blue.

To account for GPS inaccuracy we calculate a noise factor which is the relation between the players and the accuracy of the GPS. With very little noise one will have to point the phone diretly in the direction of the other player to hear him, when the accuracy drops one can hear the other player at an broader angle.
When the noise gets above a certian threshold we play a noise sound and make the sound of the other player stutter.

## Attribution

We took the sounds in the game from [freesound.org](https://freesound.org/)
- static noise: https://freesound.org/people/Jace/sounds/35291/
- vintage elecro pop loop : https://freesound.org/people/frankum/sounds/384468/

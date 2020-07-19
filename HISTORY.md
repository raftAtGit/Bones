# Bones - Version History

## 8 May 2018:
Exposed a copy of internal data structures in JointChannel.

## 3 January 2016:
Re-enabled commented out code to merge groups (instead of animations) in both importers. It's tested and working for Ogre3D importer but not tested for Collada importer (since I have no samples). Put a guard against Ogre3D skeletons which have more than 255 bones. Get angry to the idiot in jME which casts bone index to a byte! Fixed a possible bug which may cause Ogre3D bone assignments are incorrectly imported.

## 4 March 2015:
Switched to jPCT 1.29 in Android demo application. Fixed ninja.group.bones in Android demo application. Added a link to Android Studio version of Android demo application (It is maintained by community member Moodroid).

## 30 December 2014:
Added non-uniform scaling to Animated3D. Fixed a flaw in import scripts which causes commas not parsed. BonesIO.readXX methods do not throw ClassNotFoundException anymore. Joint and bone Object3D's in SkeletonDebugger are named according to corresponding joint's name in Skeleton.

## 10 June 2013:
jME OgreLoader can now load texture dimensions defined as float2. Switched to jPCT 1.26.

## 7 March 2013:
Exposed SkinData in Animated3D. To ensure encapsulation of internal data, a deep copy is returned.

## 30 July 2012:
Hopefully fixed a flaw in loading Ogre skin animations causing misplaced joints. This was happening when root joint(s) has rotation and child joints has translation.

## 28 July 2012:
Fixed jME bug causing NaN bone weights if all weights are zero. Fixed jME bug causing NPE when loading normals. Added AnimatedGroup.setVisibility(..) method to change visibility of all group. Shared geometry can cause wastefully sparse coordinate arrays, added code to compact those arrays, but the code is inactive. Not quite sure if it works correct.

## 5 January 2012:
Fixed jME that it can load Ogre meshes with shared geometry. Added support to skip null texture buffers (jME bug). Switched to jPCT 1.24. Checking vertex and UV buffers length and throwing a meaningful exception.

## 16 December 2011:
Fixed a bug causing an NPE when Blender exported Ogre models have no texture coordinates.

## 4 September 2011:
Applied the micro optimization EgonOlsen provided. Thanks Egon :)

## 29 June 2011:
BonesImporter now skips empty PoseTrack's. Added an option to samples to enable/disable texture rendering. Modified jME to preserve animation orders from mesh and skeleton files.

## 5 June 2011:
Modified Quaternion.slerp(..) method to avoid Quaternion creation.

## 24 April 2011:
jME 2.1 has a bug in loading pose animations. Fixed that.

## 18 April 2011:
Animated3D names are now written to stream. This changes stream version, formerly serialized groups should be re-serialized with BonedIO or script files.

## 17 April 2011:
Bones now imports submesh names from Collada and Ogre3D files. Added a method to AnimatedGroup to get an Animated3D by name.

## 10 March 2011:
Switched to jME 2.1. Also changed some piece of jME code to throw a meaningful exception instead of NPE.

## 9 March 2011:
Fixed classpath issue in import script files.

## 22 February 2011:
Animation of vertex normals has broken Android demo application so updated it to use latest (1.23 beta) jPCT AE jar. Added a light to Android demo scene to reflect animation of vertex normals. Added wakelock to Android demo to make it more look like a game. Cleaned some unnecessary pause-stop code in to Android demo.

## 4 February 2011:
Fixed a bug in ProceduralAnimationSample causing animation not applied to mesh.

## 15 January 2011:
Added Ogre3D and jME sources to zip.

## 27 November 2010:
Converted static filler objects in JointChannel to instance fields. This will slightly use more memory but will allow multiple threads operate on different JointChannel's. Added clone() method to SkeletonPose. Fixed incorrect documentation of ComLineArgs. Skeletal animation now updates vertex normals for proper lighting effects. However pose animation does not update normals since it is simply too expensive. Even Ogre3D does not do it, see this [post](http://www.ogre3d.org/forums/viewtopic.php?f=1&t=18829). Switched to jPCT 1.22.

## 29 September 2010:
Removed the unnecessary warning when loading a scaled Collada model. Seems as Collada's V coordinate is flipped compared to OpenGL's (see this [post](http://www.collada.org/public_forum/viewtopic.php?f=12&t=1227)). Started flipping V coordinate during loading to match OpenGL's.

## 27 May 10:
Cloning Animated3D and AnimatedGroup copies autoApplyAnimation field. Micro optimization in Animated3D.applySkeletonPose(). Relaxed the assumption joints in jME skeleton are ordered such that parents come before children. Changed the internal mechanism joints are created out of jME bones.

## 1 April 10:
Added setSkeletonPose(SkeletonPose) and removeFromWorld(World) methods to AnimatedGroup. Improved Android sample to show/animate up to 8 animated groups, added touch screen support and fast restore ability when GL surface is lost and restored again.

## 30 March 10:
Updated documentation. Fixed a minor bug causing applying skeleton pose twice. Added a sample Android application.

## 29 March 10:
Added support to load Ogre3D pose animations via jME's OgreXML loader. Renamed many classes to provide some space for new animations. Pose animations can be blended within each other and with a single skin animation. Completely decoupled jME and Ardor3D importing code into new classes. This was necessary to run Bones on Android (Dalvik refuses to load classes if there are constructors/methods which has parameters of unknown classes). Added support to scale/rotate objects/skeleton/animations at import time. Exposed some constructors, methods and data structures to allow programatically created animated objects, skeletons and animations. Added some more sample applications.

## 07 February 10:
Updated documentation. Added support to clone SkinnedGroup.

## 06 February 10:
Added support to load Ogre3D animation files (mesh.xml + skeleton.xml) via jME's OgreXMl loader. Main motivation behind this was figuring out Collada exporters for 3dsMax (both Max's built-in and OpenCollada) don't support Pyhsique modifier which is kind of annoying especially considering many of ready to buy models are skinned with Pyhsique modifier. In contrast Ogre3D has very nice exporters which both support Pyhsique and Skin modifier.

## 01 February 10:
Ardor3D now supports loading animations and animation blending (SVN pre 0.7 version). Updated Bones accordingly without blending. This time I've significantly diverged from Ardor's implementation for sake of easy usage and to match jPCT semantics.

## 07 January 10:
First release of Bones. This release is a port of Ardor3D's (0.6) animation system. Collada files are loaded via Ardor3D's Collada loader and then converted into Bones data structures. Ardor3D doesn't support loading animations and so does Bones. Only procedural animation is supported.

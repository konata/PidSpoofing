# PidSpoofing

~~failed~~ Proof of concept on pid spoofing for android

## prerequisite knowledge

Some critical android service has a loose permission check, via `Binder.getCallingPid()`
i.e [WearPackageIconProvider](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/PackageInstaller/src/com/android/packageinstaller/wear/WearPackageIconProvider.java;drc=a4812d3c587a10bd9f4b76a26b174df92ee8abb6;l=144?q=WearPackageIconProvider)

## what to exploit

We may trick the server into identifying our request is issued from a system process, thus execute
our arbitrary request

## how to achieve

1. Cast an ipc call to the android server
2. Kill ourselves
3. Employ another process to spawn and exit shell process thus to rolling the pid to someone near
   $pid-a
4. Launch a system application into $pid-a
5. The server identify the call in step 1 is issued from a system application, thus we can bypass
   the permission check

The difficultly lays that the execution of the server **must be slow enough** so that we have done
step 2/3/4 which normally takes 5 minutes or so even at our best effort

`PidSpoofing` implement some mechanism to block server execution for arbitrary seconds / hours

## limitation

Android pid reuse is not in a linear, predictable, stable way, thus I failed in the last
step `launch into a system application with designated pid`, the pid may not reuse but skip



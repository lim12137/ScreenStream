![](screenshots/about_image_full.png)
# ScreenStream

ScreenStream is an Android application for sharing the device screen to a web browser on the same local network.

This fork is being simplified into a local-first build centered on **HTTP + MJPEG** streaming. The current product direction is:

- one stream mode
- browser viewing on the same LAN, hotspot, or USB network
- no extra viewer app required
- straightforward deployment and stable local usage

<a href='https://play.google.com/store/apps/details?id=info.dvkr.screenstream'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="100"/></a> <a href="https://f-droid.org/packages/info.dvkr.screenstream/" target="_blank"><img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="100"/></a>

 * [Project support](#project-support)
 * [Stream mode](#stream-mode)
   + [Local mode (MJPEG)](#local-mode-mjpeg)
 * [HarmonyOS 4.2 notes](#harmonyos-42-notes)
 * [Screenshots](#screenshots)
 * [Contribution](#contribution)
 * [Developer](#developed-by)
 * [Privacy Policy and Terms & Conditions](#privacy-policy-and-terms--conditions)
 * [License](#license)

## Project support

If you find **ScreenStream** useful, please consider donating to support its development.<br>
Your contribution is greatly appreciated!

**Tron (TRC20) USDT / USDC :** `TUKtTz3oe3qKYmm1ScDLKjRz9ty1FahpzR`

**Solana (SPL) USDT / USDC :** `7HasyJBLpiNhFvMvbfKUDXZwThRCfc3S4Yr74Rbs5e23`

**BNB Smart Chain (BEP20) USDT / USDC / BUSD / BNB :** `0x96818c1CfE9F613d8194c882a0898B8B3c47077B`

## Stream mode

This fork currently ships a single local-network stream mode: **Local mode (MJPEG)**.

| Mode | Transport | Audio | Internet required | Server side | Security |
|------|-----------|-------|-------------------|-------------|----------|
| **Local (MJPEG)** | HTTP MJPEG | No | No | Built-in | Optional 4-digit PIN |

The number of clients is not directly limited, but each connected browser consumes CPU and bandwidth. This mode is best suited for local monitoring, demos, labs, and control-room style viewing rather than internet-scale broadcast.

The application uses Android [MediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjection) and requires Android 6.0 or higher.

> [!WARNING]
>
> - **High local network traffic**: MJPEG sends a continuous sequence of images, so bandwidth can become high, especially with multiple clients.
> - **Streaming delay**: Expect at least 0.5-1 second of delay or more on slow devices, poor networks, or under heavy CPU load.
> - **Not ideal for HD video playback**: ScreenStream is optimized for screen sharing and monitoring, not high-quality video streaming.

### Local mode (MJPEG)

Local mode is built on the MJPEG standard and uses an embedded HTTP server inside the app. Internet access is not required. It can work over Wi-Fi, hotspot, Network-over-USB, or any other local connection between the Android device and the viewer's browser.

For best results, use a fast and stable local network.

The app processes each frame independently before sending it to the browser, which makes transformations such as cropping, resizing, rotating, and other adjustments possible.

The local mode offers the following functionality:

- powered by MJPEG
- optional PIN protection
- browser-based viewing
- embedded HTTP server
- no internet dependency
- resizing by percentage or exact resolution
- IPv4 and IPv6 support
- support for Wi-Fi, hotspot, and USB network scenarios
- highly customizable settings

> [!NOTE]
>
> - Some mobile operators block incoming connections for security reasons. Even if the device has an IP address, direct access may still fail.
> - Some public or guest Wi-Fi networks block device-to-device traffic. In those environments, the viewer may not be able to reach the phone.

## HarmonyOS 4.2 notes

This fork now includes an in-app HarmonyOS 4.2 setup guide focused on reducing interrupted streams. Before first use, check:

- notification permission
- screen capture permission
- battery management or unrestricted background activity
- task locking or protected app behavior if the system supports it

If streaming stops after locking the screen or moving the app to the background, check those settings first.

## Screenshots

![](screenshots/screenshot_lm_d.png)&nbsp;![](screenshots/screenshot_lm_d_about.png)<br>
![](screenshots/screenshot_settings_1_d.png)&nbsp;![](screenshots/screenshot_settings_1_l.png)<br>
![](screenshots/screenshot_settings_2_d.png)&nbsp;![](screenshots/screenshot_settings_2_l.png)<br>
![](screenshots/screenshot_settings_3_d.png)&nbsp;![](screenshots/screenshot_settings_3_l.png)<br>
![](screenshots/screenshot_settings_4_d.png)&nbsp;![](screenshots/screenshot_settings_4_l.png)<br>
![](screenshots/screenshot_about_d.png)

## Contribution

To contribute with translation, kindly translate the following files:

1. https://github.com/dkrivoruchko/ScreenStream/blob/master/app/src/main/res/values/strings.xml
1. https://github.com/dkrivoruchko/ScreenStream/blob/master/mjpeg/src/main/res/values/strings.xml

Then, please, [make a pull request](https://help.github.com/en/articles/creating-a-pull-request) or send those translated files to the developer via e-mail <dkrivoruchko@gmail.com> as an attachment.

Your contribution is valuable and will help improve the accessibility of the application. Thank you for your efforts!

## Developed By

Developed by [Dmytro Kryvoruchko](dkrivoruchko@gmail.com). If there are any issues or ideas, feel free to contact me.

## Privacy Policy and Terms & Conditions

App [Privacy Policy](https://github.com/dkrivoruchko/ScreenStream/blob/master/PrivacyPolicy.md) and [Terms & Conditions](https://github.com/dkrivoruchko/ScreenStream/blob/master/TermsConditions.md)

## License

```
The MIT License (MIT)

Copyright (c) 2016 Dmytro Kryvoruchko

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

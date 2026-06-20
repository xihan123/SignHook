![SignHook](https://socialify.git.ci/xihan123/SignHook/image?description=1&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Auto)

![above](https://img.shields.io/badge/Android-9.0%20or%20above-brightgreen.svg)
![Xposed](https://img.shields.io/badge/Xposed%20API-101%2B-blue.svg)
[![Android CI](https://github.com/xihan123/SignHook/actions/workflows/build.yml/badge.svg)](https://github.com/xihan123/SignHook/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/release/xihan123/SignHook.svg)](https://github.com/xihan123/SignHook/releases)
![downloads](https://img.shields.io/github/downloads/xihan123/SignHook/total)

## SignHook(签名助手) Xposed模块

一个简单的签名校验通杀模块。

## 环境要求

- Android 9.0(API 28) 及以上
- LSPosed 或其他支持 libxposed 的框架

## 使用说明

- 初次进入点击刷新，获取应用列表

- 在 LSPosed 里勾选需要作用的宿主应用，比如 QQ、微信

- 找到需要伪装签名的应用，填写伪装值并启用

          符合条件的包名被宿主查询签名时，会返回填写的伪装签名

- 例如修改版应用无法调用 QQ/VX 登录，提示非官方正版

          读取官方版 APK 的签名 -> 填到修改版应用的伪装值 -> 保存并启用

          修改后强行停止 QQ/VX 等宿主应用，再重新打开即可

## 常见问题

- 不生效：检查 LSPosed 作用域，修改后强行停止宿主应用
- 签名为空：刷新应用列表，确认应用可正常读取签名
- 配置不同步：重启模块和宿主应用

## 免责声明

- 本模块仅供学习交流使用，使用者自行承担风险和责任。
- Xposed 模块会修改系统/应用行为，可能导致应用异常、设备不稳定或数据丢失。
- 请自行确认使用场景是否合法合规，任何后果与作者无关。

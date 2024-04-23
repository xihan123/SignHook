## SignHook(签名助手) Xp模块

使用 [YukiHookAPI](https://github.com/fankes/YukiHookAPI)

* ~~1.1.13及之前使用前注意给作用域包名的应用存储权限~~
* 1.1.13之后版本都不支持内置模块望周知!!!

## fix: Obtain original signature
The original signature of the installation package can be obtained, which has been verified.
The installation package must be placed in the **【/storage/mutated/0/Android/data/cn.xihan.sign/cache】** directory.

## 使用说明

* 初次进入下拉刷新即可获取应用

* 在右上角添加作用域包名并且在Lsp框架内勾选需要作用的应用

          会让所有符合条件(伪造签名值不为空以及勾选选择框)  修改签名值为伪造的签名值

* 比如我安装了一个修改版的某点阅读，无法使用QQ、VX调用登录 会提示"该应用非官方正版应用，xxx"

          使用右上角的选择apk->选中官方版的apk->复制读取到的签名值->粘贴到伪造签名

          每次修改后需要重启作用域包名的应用才会生效，调用者无需重启

          在这就是某点不用重启，强行停止QQ就行

## 常见问题

* 读取签名错误：注意要从内部存储里选择apk!!!不是"最近"或者其他地方

* 内置问题:注意是针对要登录/分享的平台，如Q、VX等，调用者无需任何处理，只要填写了正确的签名值即可

## 免责声明

* 该Xposed模块仅供学习交流使用，使用者必须自行承担使用该模块所带来的风险和责任。

* 该Xposed模块可以修改系统行为，使用者应该仔细审查模块的操作并自行决定是否使用。
* 使用该Xposed模块可能导致设备不稳定、崩溃和数据丢失等问题。作者不对任何因使用该模块而导致的问题承担责任。
* 开发者保留对该Xposed模块的更新、修改、暂停、终止等权利，使用者应该自行确认其使用版本的安全性和稳定性。
* 任何人因使用该Xposed模块而导致的任何问题，作者不承担任何责任，一切后果由使用者自行承担。
* 对于使用该Xposed模块所产生的任何问题，作者不提供任何形式的技术支持和解决方案。

请在使用该Xposed模块之前认真阅读以上免责声明并自行权衡风险和利益，如有异议请勿使用。如果您使用了该Xposed模块，即代表您已经完全接受本免责声明。

---

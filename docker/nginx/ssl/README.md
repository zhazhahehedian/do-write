# SSL 证书目录

将你的SSL证书文件放在这个目录下：

- `yourdomain.com_bundle.crt` - 证书文件
- `yourdomain.com.key` - 私钥文件

**注意**: 证书文件不会被提交到Git仓库，请妥善保管。

## 腾讯云证书下载步骤

1. 登录腾讯云控制台
2. SSL证书管理 → 找到你的证书
3. 点击"下载" → 选择 **Nginx** 类型
4. 解压得到 `.crt` 和 `.key` 文件
5. 上传到此目录

## 其他证书提供商

如果使用其他证书提供商（Let's Encrypt、阿里云等），请确保：
- 证书文件格式为 `.crt` 或 `.pem`
- 私钥文件格式为 `.key`
- 修改 `default.conf` 中的文件名
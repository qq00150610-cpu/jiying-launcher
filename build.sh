#!/bin/bash
# 极影桌面自动构建脚本

echo "======================================"
echo "  极影桌面 v2.1 自动构建脚本"
echo "======================================"
echo ""

cd "$(dirname "$0")"

# 检查当前目录
if [ ! -f "gradlew" ]; then
    echo "❌ 错误: 找不到 gradlew 文件"
    echo "请确保在项目根目录运行此脚本"
    exit 1
fi

# 清理旧的构建文件
echo "📦 清理旧的构建文件..."
rm -rf app/build 2>/dev/null || true
mkdir -p app/build

# 等待一下让文件系统稳定
sleep 2

# 检查构建环境
echo "🔍 检查构建环境..."
java -version 2>&1 | head -1
echo ""

# 构建Debug版本
echo "🔨 开始构建 Debug APK..."
./gradlew assembleDebug --no-daemon --max-workers=1

if [ $? -eq 0 ]; then
    echo "✅ Debug APK 构建成功"
    ls -la app/build/outputs/apk/debug/ 2>/dev/null
else
    echo "⚠️ Debug APK 构建失败，尝试 Release 构建..."
fi

# 构建Release版本
echo ""
echo "🔨 开始构建 Release APK..."
./gradlew assembleRelease --no-daemon --max-workers=1

if [ $? -eq 0 ]; then
    echo "✅ Release APK 构建成功"
    ls -la app/build/outputs/apk/release/ 2>/dev/null
    
    # 复制到输出目录
    mkdir -p ../apk_output
    cp app/build/outputs/apk/release/app-release.apk ../apk_output/jiying-launcher-v2.1.apk
    echo ""
    echo "📄 APK 已复制到: ../apk_output/jiying-launcher-v2.1.apk"
else
    echo "❌ Release APK 构建失败"
    echo ""
    echo "💡 提示: 如果遇到文件系统问题，请尝试:"
    echo "  1. 重启系统后重试"
    echo "  2. 使用 Android Studio 构建"
    echo "  3. 使用 GitHub Actions 构建"
    exit 1
fi

echo ""
echo "======================================"
echo "  构建完成！"
echo "======================================"

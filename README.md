```text
      _____ _     _
  __ /__   \ |__ (_)_ __   __ _
 / _` |/ /\/ '_ \| | '_ \ / _` |
| (_| / /  | | | | | | | | (_| |
 \__,_\/   |_| |_|_|_| |_|\__, |
                          |___/

Just a Thing
```

# 远程升级

## 框架使用

### 添加仓库

```xml
<!-- pom.xml增加仓库 -->
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/athingx/athing-upgrade</url>
    </repository>
</repositories>
```

### 构建客户端

```xml
<!-- pom.xml增加引用 -->
<dependency>
    <groupId>io.github.athingx.athing</groupId>
    <artifactId>athing-upgrade-thing</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```java
// 构建设备模型
final var thingUpgrade = new ThingUpgradeBuilder()
        .build(thing)
        .get();

// 上报模块版本
thingUpgrade.inform("resource", "1.0.0")
        .whenComplete((unused, cause) -> {
            // 处理后续
        });
```

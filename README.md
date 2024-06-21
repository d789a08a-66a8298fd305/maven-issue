# 问题 1: 存在依赖的模块必须声明 `--also-make`


下面两个命令都会失败，因为 `module2` 依赖于 `module1`，但是没有声明 `--also-make` 选项。
通常我在 CI 中可能会执行第二个命令, 因为 test 会强制 compile，在 maven lifecycle 里在 compile 之后

```bash
mvn clean compile -pl module2 
mvn clean test -pl module2 
```

## 问题 2: 简单加 `-am` 选项就可以解决, 但会执行 module1 的测试

我在 module1 的单测里，执行了阻塞 60s，如果下面的命令执行超过 5s，对编写 CI 的人是一个负担。

原因是 maven 没有提供一个选项，声明 module2 的 test 会依赖 module1 的 test 吗？

```bash
mvn clean test -pl module2 -am
```


## 猜想 1: 将 compile 和 test 分开执行

假如 module2 依赖 module1，但 test 其实是不依赖的（maven 能做到 test 依赖，但是会用到一个 test-jar，这个也支持的不是很友好)
这样我们可以将步骤分开，module2 只需要 module1 的编译结果，那么我们提前编译就不会出现问题了

```bash
mvn clean compile -pl module2 -am
mvn test -pl module2
```

尝试执行上面的命令，发现并没有解决问题，maven 无法找到 module1 的 jar 包（具体原因未深入，大概率是命令不在同一个 maven 生命周期）

## 猜想 2: 编译后再用 -am 不会执行 module1 的 test

假如 `mvn clean test -pl module2 -am` 是因为 module2 的 compile 依赖了 module1 的测试，而 test 并不会让 maven 在两者之间产生依赖关系，
那么我们可以先编译 module2 和 module1，再执行 `test -am`，这样就不会执行 module1 的测试了。

实际上也不行，maven 没有定义 Dependency 是依赖的测试，还是编译

```bash
mvn clean compile -pl module2 -am
mvn test -pl module2 -am
```

## 结论

maven 没有细化依赖的类型，导致我们无法在编译和测试以及其他生命周期阶段之间做到细粒度的依赖控制。

基于 `-am` 执行某个生命周期，就一定会执行依赖模块的所有生命周期，例如对于较高级别的 install，则会先执行 Module1 的所有生命周期，我们通过日志来诊断一下:

我把日志精简了一下，对于 module1 而言，一个 module2 的 install 竟然会执行 module1 的所有生命周期，这是不合理的：


```log
[INFO] ------------------------< org.example:module1 >-------------------------
[INFO] Building module1 1.0-SNAPSHOT                                      [2/3]
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ module1 ---[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ module1 ---
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ module1 ---
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ module1 ---
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ module1 ---
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ module1 ---
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ module1 ---
[INFO] --- maven-install-plugin:2.4:install (default-install) @ module1 ---
```

下面是精简的完整生命周期，可以看到 surefire 插件用于执行了 test

```log
[INFO] ----------------------< org.example:maven-issue >-----------------------
[INFO] Building maven-issue 1.0-SNAPSHOT                                  [1/3]
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ maven-issue ---
[INFO] --- maven-install-plugin:2.4:install (default-install) @ maven-issue ---
[INFO] 
[INFO] ------------------------< org.example:module1 >-------------------------
[INFO] Building module1 1.0-SNAPSHOT                                      [2/3]
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ module1 ---[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ module1 ---
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ module1 ---
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ module1 ---
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ module1 ---
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ module1 ---
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ module1 ---
[INFO] --- maven-install-plugin:2.4:install (default-install) @ module1 ---
[INFO] 
[INFO] ------------------------< org.example:module2 >-------------------------
[INFO] Building module2 1.0-SNAPSHOT                                      [3/3]
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ module2 ---
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ module2 ---
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ module2 ---
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ module2 ---
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ module2 ---
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ module2 ---
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ module2 ---
[INFO] --- maven-install-plugin:2.4:install (default-install) @ module2 ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for maven-issue 1.0-SNAPSHOT:
[INFO] 
[INFO] maven-issue ........................................ SUCCESS [  0.255 s]
[INFO] module1 ............................................ SUCCESS [  2.929 s]
[INFO] module2 ............................................ SUCCESS [  0.318 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.595 s
[INFO] Finished at: 2024-06-21T11:06:07+08:00
[INFO] -----------------------------------------------------------------------
```


## 附录1: test-jar 场景解释

简单来说就是 module1 定义一个 AbstractTest 让 module2 直接继承，着在集成测试中很常见：

- module1 实现通用逻辑
- module2 提供了不同存储的实现，例如 JDBC 的存储层实现
- module3 提供 Kafka 的存储层实现

这种情况下，AbstractTest 只需要定义单测逻辑，`KafkaTest extend AbstractTest` 只需要传入类似于接口实现以及连接 Kafka 的逻辑就可以.

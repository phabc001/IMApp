在 Kotlin 中，虽然 `messageFlow` 是在 `onCreate` 方法中定义的局部变量，但它的 **作用域和生命周期** 会因为 **闭包（closure）** 的特性而显得像是全局变量。

---

### 原因分析

#### 1. **闭包的特性**
- **闭包** 是指：
  一个函数或 Lambda 表达式可以捕获其外部作用域的变量，即使这个变量的作用域已经结束。
- 在你的代码中：
    - `messageFlow` 是 `onCreate` 方法中的局部变量。
    - 当 `messageFlow.update` 被传递到 `WebSocketManager.setOnMessageReceivedCallback` 的 Lambda 表达式中时，这个 Lambda 捕获了 `messageFlow`。
    - 因此，即使 `onCreate` 方法结束，`messageFlow` 的引用仍然可以在回调中被使用。

#### 2. **WebSocketManager 的回调**
- 回调的作用：
    - `WebSocketManager.setOnMessageReceivedCallback` 会保存这个回调函数，并在收到消息时调用它。
    - 这个回调函数中使用了 `messageFlow`。
    - 因为 `messageFlow` 被闭包捕获，它不会被销毁，而是随着回调函数的生命周期一起存在。

#### 3. **MutableStateFlow 的特性**
- `MutableStateFlow` 是一个可观察的数据流，其生命周期由持有它的对象决定。
- 在你的代码中：
    - 只要回调函数存在，`messageFlow` 的引用就一直有效，因此可以正常更新消息流。

---

### 实际代码执行的生命周期
1. **`onCreate` 方法中：**
    - 定义了局部变量 `messageFlow`。
    - 将一个使用了 `messageFlow` 的 Lambda 表达式传递给 `WebSocketManager.setOnMessageReceivedCallback`。

2. **WebSocket 回调触发时：**
    - `WebSocketManager` 调用保存的 Lambda 表达式。
    - 因为 `messageFlow` 被闭包捕获，Lambda 内部可以继续访问和操作 `messageFlow`。

---

### 为什么看起来像是全局的？
- `messageFlow` 并不是全局变量，但因为 **闭包** 捕获了它，导致它的生命周期延长到和 `WebSocketManager` 的回调相同的范围。
- 所以，在应用的上下文中，`messageFlow` 的行为像是全局的。

---

### 关键总结
- **闭包** 是使局部变量在函数外可访问的原因。
- **回调的生命周期** 决定了局部变量 `messageFlow` 的存活时间。
- 这种写法既避免了全局变量的滥用，又保持了数据的可控性，是一种高效且安全的设计模式。
トレイトで定義されている未実装のメソッドを全てNotImplementedErrorで実装するマクロ

### 使い方

モック対象のトレイトの型`T`を使って`MockFactory.mock[T]`とするだけ。

例:

```
case class Hoge(id: Int)

trait HogeRepository {
  def findX(id: Int): Hoge

  def defaultImpl(id: Int) = 1
}

@experimental
@main def test: Unit = {
  trait MockedHogeRepository extends HogeRepository {
    // ここでモックしたいメソッドをオーバーライドする
  }

  val mocked = MockFactory.mock[MockedHogeRepository] // ここで未実装のメソッドが全てNotImplementedErrorで実装される
  /**
   * ↓ MockFactory.mock[MockedHogeRepository]のマクロ展開結果
   *
   * {
   * class MockTargetImpl extends java.lang.Object with MockedHogeRepository {
   * def findX(id: scala.Int): macromock.Hoge = throw new scala.NotImplementedError("Not mocked")
   * }
   *
   * (new MockTargetImpl(): MockedHogeRepository)
   * }
   */

  println(mocked.defaultImpl(111)) // 1
  println(scala.util.Try(mocked.findX(1))) // Failure(scala.NotImplementedError: Not mocked)
}
```


### メモ

```
// ↓こんな感じで書きたい
val mocked = mock[A] (
  _.add(1, 2) -> 3, // IDEのコード補完が効くように...
  _.add(3, 4) -> 7,
)
```

```
// ↓モックされた値を上書きするメソッドが欲しい

val mocked = mock[MockedHogeRepository] // ここで未実装のメソッドが全てNotImplementedErrorで実装される

update(mocked)(_.findX(1) -> new Hoge)
// ↓マクロ展開結果
{
  class MockTargetImpl extends java.lang.Object with MockedHogeRepository {
    def find(id: scala.Int): Hoge = id match {
      case 1 => new Hoge
      case _ => mocked.find(id) // オリジナルのmockの方に処理を委譲
    }
  }
  
  new MockTargetImpl
}
```
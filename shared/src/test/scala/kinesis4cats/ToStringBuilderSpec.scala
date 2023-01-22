package kinesis4cats

class ToStringBuilderSpec extends munit.FunSuite {

  case class TestData(s: String, d: Double, l: Long, b: Boolean) {
    override def toString(): String = ToStringBuilder("TestData")
      .add("s", s)
      .add("d", d)
      .add("l", l)
      .add("b", b)
      .build
  }

  test("It should build a string properly") {
    assertEquals(
      TestData("foo", 2.0, 5, true).toString(),
      "TestData(s=foo,d=2.0,l=5,b=true)"
    )
  }
}

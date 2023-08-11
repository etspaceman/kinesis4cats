/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.compat

package functionConverterImpls {
  class FromJavaBiConsumer[T, U](jf: java.util.function.BiConsumer[T, U])
      extends scala.Function2[T, U, Unit] {
    def apply(x1: T, x2: U) = jf.accept(x1, x2)
  }

  class RichBiConsumerAsFunction2[T, U](
      private val underlying: java.util.function.BiConsumer[T, U]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, Unit] =
      new FromJavaBiConsumer[T, U](underlying)
  }

  class AsJavaBiConsumer[T, U](sf: scala.Function2[T, U, Unit])
      extends java.util.function.BiConsumer[T, U] {
    def accept(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsBiConsumer[T, U](
      private val underlying: scala.Function2[T, U, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.BiConsumer[T, U] =
      new AsJavaBiConsumer[T, U](underlying)
  }

  class FromJavaBiFunction[T, U, R](jf: java.util.function.BiFunction[T, U, R])
      extends scala.Function2[T, U, R] {
    def apply(x1: T, x2: U) = jf.apply(x1, x2)
  }

  class RichBiFunctionAsFunction2[T, U, R](
      private val underlying: java.util.function.BiFunction[T, U, R]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, R] =
      new FromJavaBiFunction[T, U, R](underlying)
  }

  class AsJavaBiFunction[T, U, R](sf: scala.Function2[T, U, R])
      extends java.util.function.BiFunction[T, U, R] {
    def apply(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsBiFunction[T, U, R](
      private val underlying: scala.Function2[T, U, R]
  ) extends AnyVal {
    @inline def asJava: java.util.function.BiFunction[T, U, R] =
      new AsJavaBiFunction[T, U, R](underlying)
  }

  class FromJavaBiPredicate[T, U](jf: java.util.function.BiPredicate[T, U])
      extends scala.Function2[T, U, Boolean] {
    def apply(x1: T, x2: U) = jf.test(x1, x2)
  }

  class RichBiPredicateAsFunction2[T, U](
      private val underlying: java.util.function.BiPredicate[T, U]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, Boolean] =
      new FromJavaBiPredicate[T, U](underlying)
  }

  class AsJavaBiPredicate[T, U](sf: scala.Function2[T, U, Boolean])
      extends java.util.function.BiPredicate[T, U] {
    def test(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsBiPredicate[T, U](
      private val underlying: scala.Function2[T, U, Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.BiPredicate[T, U] =
      new AsJavaBiPredicate[T, U](underlying)
  }

  class FromJavaBinaryOperator[T](jf: java.util.function.BinaryOperator[T])
      extends scala.Function2[T, T, T] {
    def apply(x1: T, x2: T) = jf.apply(x1, x2)
  }

  class RichBinaryOperatorAsFunction2[T](
      private val underlying: java.util.function.BinaryOperator[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, T, T] =
      new FromJavaBinaryOperator[T](underlying)
  }

  class AsJavaBinaryOperator[T](sf: scala.Function2[T, T, T])
      extends java.util.function.BinaryOperator[T] {
    def apply(x1: T, x2: T) = sf.apply(x1, x2)
  }

  class RichFunction2AsBinaryOperator[T](
      private val underlying: scala.Function2[T, T, T]
  ) extends AnyVal {
    @inline def asJava: java.util.function.BinaryOperator[T] =
      new AsJavaBinaryOperator[T](underlying)
  }

  class FromJavaBooleanSupplier(jf: java.util.function.BooleanSupplier)
      extends scala.Function0[Boolean] {
    def apply() = jf.getAsBoolean()
  }

  class RichBooleanSupplierAsFunction0(
      private val underlying: java.util.function.BooleanSupplier
  ) extends AnyVal {
    @inline def asScala: scala.Function0[Boolean] = new FromJavaBooleanSupplier(
      underlying
    )
  }

  class AsJavaBooleanSupplier(sf: scala.Function0[Boolean])
      extends java.util.function.BooleanSupplier {
    def getAsBoolean() = sf.apply()
  }

  class RichFunction0AsBooleanSupplier(
      private val underlying: scala.Function0[Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.BooleanSupplier =
      new AsJavaBooleanSupplier(underlying)
  }

  class FromJavaConsumer[T](jf: java.util.function.Consumer[T])
      extends scala.Function1[T, Unit] {
    def apply(x1: T) = jf.accept(x1)
  }

  class RichConsumerAsFunction1[T](
      private val underlying: java.util.function.Consumer[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, Unit] =
      new FromJavaConsumer[T](underlying)
  }

  class AsJavaConsumer[T](sf: scala.Function1[T, Unit])
      extends java.util.function.Consumer[T] {
    def accept(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsConsumer[T](
      private val underlying: scala.Function1[T, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.Consumer[T] =
      new AsJavaConsumer[T](underlying)
  }

  class FromJavaDoubleBinaryOperator(
      jf: java.util.function.DoubleBinaryOperator
  ) extends scala.Function2[Double, Double, Double] {
    def apply(x1: scala.Double, x2: scala.Double) = jf.applyAsDouble(x1, x2)
  }

  class RichDoubleBinaryOperatorAsFunction2(
      private val underlying: java.util.function.DoubleBinaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function2[Double, Double, Double] =
      new FromJavaDoubleBinaryOperator(underlying)
  }

  class AsJavaDoubleBinaryOperator(sf: scala.Function2[Double, Double, Double])
      extends java.util.function.DoubleBinaryOperator {
    def applyAsDouble(x1: scala.Double, x2: scala.Double) = sf.apply(x1, x2)
  }

  class RichFunction2AsDoubleBinaryOperator(
      private val underlying: scala.Function2[Double, Double, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleBinaryOperator =
      new AsJavaDoubleBinaryOperator(underlying)
  }

  class FromJavaDoubleConsumer(jf: java.util.function.DoubleConsumer)
      extends scala.Function1[Double, Unit] {
    def apply(x1: scala.Double) = jf.accept(x1)
  }

  class RichDoubleConsumerAsFunction1(
      private val underlying: java.util.function.DoubleConsumer
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, Unit] =
      new FromJavaDoubleConsumer(underlying)
  }

  class AsJavaDoubleConsumer(sf: scala.Function1[Double, Unit])
      extends java.util.function.DoubleConsumer {
    def accept(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoubleConsumer(
      private val underlying: scala.Function1[Double, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleConsumer =
      new AsJavaDoubleConsumer(underlying)
  }

  class FromJavaDoubleFunction[R](jf: java.util.function.DoubleFunction[R])
      extends scala.Function1[Double, R] {
    def apply(x1: scala.Double) = jf.apply(x1)
  }

  class RichDoubleFunctionAsFunction1[R](
      private val underlying: java.util.function.DoubleFunction[R]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, R] =
      new FromJavaDoubleFunction[R](underlying)
  }

  class AsJavaDoubleFunction[R](sf: scala.Function1[Double, R])
      extends java.util.function.DoubleFunction[R] {
    def apply(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoubleFunction[R](
      private val underlying: scala.Function1[Double, R]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleFunction[R] =
      new AsJavaDoubleFunction[R](underlying)
  }

  class FromJavaDoublePredicate(jf: java.util.function.DoublePredicate)
      extends scala.Function1[Double, Boolean] {
    def apply(x1: scala.Double) = jf.test(x1)
  }

  class RichDoublePredicateAsFunction1(
      private val underlying: java.util.function.DoublePredicate
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, Boolean] =
      new FromJavaDoublePredicate(underlying)
  }

  class AsJavaDoublePredicate(sf: scala.Function1[Double, Boolean])
      extends java.util.function.DoublePredicate {
    def test(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoublePredicate(
      private val underlying: scala.Function1[Double, Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoublePredicate =
      new AsJavaDoublePredicate(underlying)
  }

  class FromJavaDoubleSupplier(jf: java.util.function.DoubleSupplier)
      extends scala.Function0[Double] {
    def apply() = jf.getAsDouble()
  }

  class RichDoubleSupplierAsFunction0(
      private val underlying: java.util.function.DoubleSupplier
  ) extends AnyVal {
    @inline def asScala: scala.Function0[Double] = new FromJavaDoubleSupplier(
      underlying
    )
  }

  class AsJavaDoubleSupplier(sf: scala.Function0[Double])
      extends java.util.function.DoubleSupplier {
    def getAsDouble() = sf.apply()
  }

  class RichFunction0AsDoubleSupplier(
      private val underlying: scala.Function0[Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleSupplier =
      new AsJavaDoubleSupplier(underlying)
  }

  class FromJavaDoubleToIntFunction(jf: java.util.function.DoubleToIntFunction)
      extends scala.Function1[Double, Int] {
    def apply(x1: scala.Double) = jf.applyAsInt(x1)
  }

  class RichDoubleToIntFunctionAsFunction1(
      private val underlying: java.util.function.DoubleToIntFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, Int] =
      new FromJavaDoubleToIntFunction(underlying)
  }

  class AsJavaDoubleToIntFunction(sf: scala.Function1[Double, Int])
      extends java.util.function.DoubleToIntFunction {
    def applyAsInt(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoubleToIntFunction(
      private val underlying: scala.Function1[Double, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleToIntFunction =
      new AsJavaDoubleToIntFunction(underlying)
  }

  class FromJavaDoubleToLongFunction(
      jf: java.util.function.DoubleToLongFunction
  ) extends scala.Function1[Double, Long] {
    def apply(x1: scala.Double) = jf.applyAsLong(x1)
  }

  class RichDoubleToLongFunctionAsFunction1(
      private val underlying: java.util.function.DoubleToLongFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, Long] =
      new FromJavaDoubleToLongFunction(underlying)
  }

  class AsJavaDoubleToLongFunction(sf: scala.Function1[Double, Long])
      extends java.util.function.DoubleToLongFunction {
    def applyAsLong(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoubleToLongFunction(
      private val underlying: scala.Function1[Double, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleToLongFunction =
      new AsJavaDoubleToLongFunction(underlying)
  }

  class FromJavaDoubleUnaryOperator(jf: java.util.function.DoubleUnaryOperator)
      extends scala.Function1[Double, Double] {
    def apply(x1: scala.Double) = jf.applyAsDouble(x1)
  }

  class RichDoubleUnaryOperatorAsFunction1(
      private val underlying: java.util.function.DoubleUnaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Double, Double] =
      new FromJavaDoubleUnaryOperator(underlying)
  }

  class AsJavaDoubleUnaryOperator(sf: scala.Function1[Double, Double])
      extends java.util.function.DoubleUnaryOperator {
    def applyAsDouble(x1: scala.Double) = sf.apply(x1)
  }

  class RichFunction1AsDoubleUnaryOperator(
      private val underlying: scala.Function1[Double, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.DoubleUnaryOperator =
      new AsJavaDoubleUnaryOperator(underlying)
  }

  class FromJavaFunction[T, R](jf: java.util.function.Function[T, R])
      extends scala.Function1[T, R] {
    def apply(x1: T) = jf.apply(x1)
  }

  class RichFunctionAsFunction1[T, R](
      private val underlying: java.util.function.Function[T, R]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, R] =
      new FromJavaFunction[T, R](underlying)
  }

  class AsJavaFunction[T, R](sf: scala.Function1[T, R])
      extends java.util.function.Function[T, R] {
    def apply(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsFunction[T, R](
      private val underlying: scala.Function1[T, R]
  ) extends AnyVal {
    @inline def asJava: java.util.function.Function[T, R] =
      new AsJavaFunction[T, R](underlying)
  }

  class FromJavaIntBinaryOperator(jf: java.util.function.IntBinaryOperator)
      extends scala.Function2[Int, Int, Int] {
    def apply(x1: scala.Int, x2: scala.Int) = jf.applyAsInt(x1, x2)
  }

  class RichIntBinaryOperatorAsFunction2(
      private val underlying: java.util.function.IntBinaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function2[Int, Int, Int] =
      new FromJavaIntBinaryOperator(underlying)
  }

  class AsJavaIntBinaryOperator(sf: scala.Function2[Int, Int, Int])
      extends java.util.function.IntBinaryOperator {
    def applyAsInt(x1: scala.Int, x2: scala.Int) = sf.apply(x1, x2)
  }

  class RichFunction2AsIntBinaryOperator(
      private val underlying: scala.Function2[Int, Int, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntBinaryOperator =
      new AsJavaIntBinaryOperator(underlying)
  }

  class FromJavaIntConsumer(jf: java.util.function.IntConsumer)
      extends scala.Function1[Int, Unit] {
    def apply(x1: scala.Int) = jf.accept(x1)
  }

  class RichIntConsumerAsFunction1(
      private val underlying: java.util.function.IntConsumer
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, Unit] = new FromJavaIntConsumer(
      underlying
    )
  }

  class AsJavaIntConsumer(sf: scala.Function1[Int, Unit])
      extends java.util.function.IntConsumer {
    def accept(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntConsumer(
      private val underlying: scala.Function1[Int, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntConsumer = new AsJavaIntConsumer(
      underlying
    )
  }

  class FromJavaIntFunction[R](jf: java.util.function.IntFunction[R])
      extends scala.Function1[Int, R] {
    def apply(x1: scala.Int) = jf.apply(x1)
  }

  class RichIntFunctionAsFunction1[R](
      private val underlying: java.util.function.IntFunction[R]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, R] =
      new FromJavaIntFunction[R](underlying)
  }

  class AsJavaIntFunction[R](sf: scala.Function1[Int, R])
      extends java.util.function.IntFunction[R] {
    def apply(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntFunction[R](
      private val underlying: scala.Function1[Int, R]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntFunction[R] =
      new AsJavaIntFunction[R](underlying)
  }

  class FromJavaIntPredicate(jf: java.util.function.IntPredicate)
      extends scala.Function1[Int, Boolean] {
    def apply(x1: scala.Int) = jf.test(x1)
  }

  class RichIntPredicateAsFunction1(
      private val underlying: java.util.function.IntPredicate
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, Boolean] =
      new FromJavaIntPredicate(underlying)
  }

  class AsJavaIntPredicate(sf: scala.Function1[Int, Boolean])
      extends java.util.function.IntPredicate {
    def test(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntPredicate(
      private val underlying: scala.Function1[Int, Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntPredicate =
      new AsJavaIntPredicate(underlying)
  }

  class FromJavaIntSupplier(jf: java.util.function.IntSupplier)
      extends scala.Function0[Int] {
    def apply() = jf.getAsInt()
  }

  class RichIntSupplierAsFunction0(
      private val underlying: java.util.function.IntSupplier
  ) extends AnyVal {
    @inline def asScala: scala.Function0[Int] = new FromJavaIntSupplier(
      underlying
    )
  }

  class AsJavaIntSupplier(sf: scala.Function0[Int])
      extends java.util.function.IntSupplier {
    def getAsInt() = sf.apply()
  }

  class RichFunction0AsIntSupplier(private val underlying: scala.Function0[Int])
      extends AnyVal {
    @inline def asJava: java.util.function.IntSupplier = new AsJavaIntSupplier(
      underlying
    )
  }

  class FromJavaIntToDoubleFunction(jf: java.util.function.IntToDoubleFunction)
      extends scala.Function1[Int, Double] {
    def apply(x1: scala.Int) = jf.applyAsDouble(x1)
  }

  class RichIntToDoubleFunctionAsFunction1(
      private val underlying: java.util.function.IntToDoubleFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, Double] =
      new FromJavaIntToDoubleFunction(underlying)
  }

  class AsJavaIntToDoubleFunction(sf: scala.Function1[Int, Double])
      extends java.util.function.IntToDoubleFunction {
    def applyAsDouble(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntToDoubleFunction(
      private val underlying: scala.Function1[Int, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntToDoubleFunction =
      new AsJavaIntToDoubleFunction(underlying)
  }

  class FromJavaIntToLongFunction(jf: java.util.function.IntToLongFunction)
      extends scala.Function1[Int, Long] {
    def apply(x1: scala.Int) = jf.applyAsLong(x1)
  }

  class RichIntToLongFunctionAsFunction1(
      private val underlying: java.util.function.IntToLongFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, Long] =
      new FromJavaIntToLongFunction(underlying)
  }

  class AsJavaIntToLongFunction(sf: scala.Function1[Int, Long])
      extends java.util.function.IntToLongFunction {
    def applyAsLong(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntToLongFunction(
      private val underlying: scala.Function1[Int, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntToLongFunction =
      new AsJavaIntToLongFunction(underlying)
  }

  class FromJavaIntUnaryOperator(jf: java.util.function.IntUnaryOperator)
      extends scala.Function1[Int, Int] {
    def apply(x1: scala.Int) = jf.applyAsInt(x1)
  }

  class RichIntUnaryOperatorAsFunction1(
      private val underlying: java.util.function.IntUnaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Int, Int] =
      new FromJavaIntUnaryOperator(underlying)
  }

  class AsJavaIntUnaryOperator(sf: scala.Function1[Int, Int])
      extends java.util.function.IntUnaryOperator {
    def applyAsInt(x1: scala.Int) = sf.apply(x1)
  }

  class RichFunction1AsIntUnaryOperator(
      private val underlying: scala.Function1[Int, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.IntUnaryOperator =
      new AsJavaIntUnaryOperator(underlying)
  }

  class FromJavaLongBinaryOperator(jf: java.util.function.LongBinaryOperator)
      extends scala.Function2[Long, Long, Long] {
    def apply(x1: scala.Long, x2: scala.Long) = jf.applyAsLong(x1, x2)
  }

  class RichLongBinaryOperatorAsFunction2(
      private val underlying: java.util.function.LongBinaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function2[Long, Long, Long] =
      new FromJavaLongBinaryOperator(underlying)
  }

  class AsJavaLongBinaryOperator(sf: scala.Function2[Long, Long, Long])
      extends java.util.function.LongBinaryOperator {
    def applyAsLong(x1: scala.Long, x2: scala.Long) = sf.apply(x1, x2)
  }

  class RichFunction2AsLongBinaryOperator(
      private val underlying: scala.Function2[Long, Long, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongBinaryOperator =
      new AsJavaLongBinaryOperator(underlying)
  }

  class FromJavaLongConsumer(jf: java.util.function.LongConsumer)
      extends scala.Function1[Long, Unit] {
    def apply(x1: scala.Long) = jf.accept(x1)
  }

  class RichLongConsumerAsFunction1(
      private val underlying: java.util.function.LongConsumer
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, Unit] = new FromJavaLongConsumer(
      underlying
    )
  }

  class AsJavaLongConsumer(sf: scala.Function1[Long, Unit])
      extends java.util.function.LongConsumer {
    def accept(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongConsumer(
      private val underlying: scala.Function1[Long, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongConsumer =
      new AsJavaLongConsumer(underlying)
  }

  class FromJavaLongFunction[R](jf: java.util.function.LongFunction[R])
      extends scala.Function1[Long, R] {
    def apply(x1: scala.Long) = jf.apply(x1)
  }

  class RichLongFunctionAsFunction1[R](
      private val underlying: java.util.function.LongFunction[R]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, R] =
      new FromJavaLongFunction[R](underlying)
  }

  class AsJavaLongFunction[R](sf: scala.Function1[Long, R])
      extends java.util.function.LongFunction[R] {
    def apply(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongFunction[R](
      private val underlying: scala.Function1[Long, R]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongFunction[R] =
      new AsJavaLongFunction[R](underlying)
  }

  class FromJavaLongPredicate(jf: java.util.function.LongPredicate)
      extends scala.Function1[Long, Boolean] {
    def apply(x1: scala.Long) = jf.test(x1)
  }

  class RichLongPredicateAsFunction1(
      private val underlying: java.util.function.LongPredicate
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, Boolean] =
      new FromJavaLongPredicate(underlying)
  }

  class AsJavaLongPredicate(sf: scala.Function1[Long, Boolean])
      extends java.util.function.LongPredicate {
    def test(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongPredicate(
      private val underlying: scala.Function1[Long, Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongPredicate =
      new AsJavaLongPredicate(underlying)
  }

  class FromJavaLongSupplier(jf: java.util.function.LongSupplier)
      extends scala.Function0[Long] {
    def apply() = jf.getAsLong()
  }

  class RichLongSupplierAsFunction0(
      private val underlying: java.util.function.LongSupplier
  ) extends AnyVal {
    @inline def asScala: scala.Function0[Long] = new FromJavaLongSupplier(
      underlying
    )
  }

  class AsJavaLongSupplier(sf: scala.Function0[Long])
      extends java.util.function.LongSupplier {
    def getAsLong() = sf.apply()
  }

  class RichFunction0AsLongSupplier(
      private val underlying: scala.Function0[Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongSupplier =
      new AsJavaLongSupplier(underlying)
  }

  class FromJavaLongToDoubleFunction(
      jf: java.util.function.LongToDoubleFunction
  ) extends scala.Function1[Long, Double] {
    def apply(x1: scala.Long) = jf.applyAsDouble(x1)
  }

  class RichLongToDoubleFunctionAsFunction1(
      private val underlying: java.util.function.LongToDoubleFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, Double] =
      new FromJavaLongToDoubleFunction(underlying)
  }

  class AsJavaLongToDoubleFunction(sf: scala.Function1[Long, Double])
      extends java.util.function.LongToDoubleFunction {
    def applyAsDouble(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongToDoubleFunction(
      private val underlying: scala.Function1[Long, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongToDoubleFunction =
      new AsJavaLongToDoubleFunction(underlying)
  }

  class FromJavaLongToIntFunction(jf: java.util.function.LongToIntFunction)
      extends scala.Function1[Long, Int] {
    def apply(x1: scala.Long) = jf.applyAsInt(x1)
  }

  class RichLongToIntFunctionAsFunction1(
      private val underlying: java.util.function.LongToIntFunction
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, Int] =
      new FromJavaLongToIntFunction(underlying)
  }

  class AsJavaLongToIntFunction(sf: scala.Function1[Long, Int])
      extends java.util.function.LongToIntFunction {
    def applyAsInt(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongToIntFunction(
      private val underlying: scala.Function1[Long, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongToIntFunction =
      new AsJavaLongToIntFunction(underlying)
  }

  class FromJavaLongUnaryOperator(jf: java.util.function.LongUnaryOperator)
      extends scala.Function1[Long, Long] {
    def apply(x1: scala.Long) = jf.applyAsLong(x1)
  }

  class RichLongUnaryOperatorAsFunction1(
      private val underlying: java.util.function.LongUnaryOperator
  ) extends AnyVal {
    @inline def asScala: scala.Function1[Long, Long] =
      new FromJavaLongUnaryOperator(underlying)
  }

  class AsJavaLongUnaryOperator(sf: scala.Function1[Long, Long])
      extends java.util.function.LongUnaryOperator {
    def applyAsLong(x1: scala.Long) = sf.apply(x1)
  }

  class RichFunction1AsLongUnaryOperator(
      private val underlying: scala.Function1[Long, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.LongUnaryOperator =
      new AsJavaLongUnaryOperator(underlying)
  }

  class FromJavaObjDoubleConsumer[T](
      jf: java.util.function.ObjDoubleConsumer[T]
  ) extends scala.Function2[T, Double, Unit] {
    def apply(x1: T, x2: scala.Double) = jf.accept(x1, x2)
  }

  class RichObjDoubleConsumerAsFunction2[T](
      private val underlying: java.util.function.ObjDoubleConsumer[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, Double, Unit] =
      new FromJavaObjDoubleConsumer[T](underlying)
  }

  class AsJavaObjDoubleConsumer[T](sf: scala.Function2[T, Double, Unit])
      extends java.util.function.ObjDoubleConsumer[T] {
    def accept(x1: T, x2: scala.Double) = sf.apply(x1, x2)
  }

  class RichFunction2AsObjDoubleConsumer[T](
      private val underlying: scala.Function2[T, Double, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ObjDoubleConsumer[T] =
      new AsJavaObjDoubleConsumer[T](underlying)
  }

  class FromJavaObjIntConsumer[T](jf: java.util.function.ObjIntConsumer[T])
      extends scala.Function2[T, Int, Unit] {
    def apply(x1: T, x2: scala.Int) = jf.accept(x1, x2)
  }

  class RichObjIntConsumerAsFunction2[T](
      private val underlying: java.util.function.ObjIntConsumer[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, Int, Unit] =
      new FromJavaObjIntConsumer[T](underlying)
  }

  class AsJavaObjIntConsumer[T](sf: scala.Function2[T, Int, Unit])
      extends java.util.function.ObjIntConsumer[T] {
    def accept(x1: T, x2: scala.Int) = sf.apply(x1, x2)
  }

  class RichFunction2AsObjIntConsumer[T](
      private val underlying: scala.Function2[T, Int, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ObjIntConsumer[T] =
      new AsJavaObjIntConsumer[T](underlying)
  }

  class FromJavaObjLongConsumer[T](jf: java.util.function.ObjLongConsumer[T])
      extends scala.Function2[T, Long, Unit] {
    def apply(x1: T, x2: scala.Long) = jf.accept(x1, x2)
  }

  class RichObjLongConsumerAsFunction2[T](
      private val underlying: java.util.function.ObjLongConsumer[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, Long, Unit] =
      new FromJavaObjLongConsumer[T](underlying)
  }

  class AsJavaObjLongConsumer[T](sf: scala.Function2[T, Long, Unit])
      extends java.util.function.ObjLongConsumer[T] {
    def accept(x1: T, x2: scala.Long) = sf.apply(x1, x2)
  }

  class RichFunction2AsObjLongConsumer[T](
      private val underlying: scala.Function2[T, Long, Unit]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ObjLongConsumer[T] =
      new AsJavaObjLongConsumer[T](underlying)
  }

  class FromJavaPredicate[T](jf: java.util.function.Predicate[T])
      extends scala.Function1[T, Boolean] {
    def apply(x1: T) = jf.test(x1)
  }

  class RichPredicateAsFunction1[T](
      private val underlying: java.util.function.Predicate[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, Boolean] =
      new FromJavaPredicate[T](underlying)
  }

  class AsJavaPredicate[T](sf: scala.Function1[T, Boolean])
      extends java.util.function.Predicate[T] {
    def test(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsPredicate[T](
      private val underlying: scala.Function1[T, Boolean]
  ) extends AnyVal {
    @inline def asJava: java.util.function.Predicate[T] =
      new AsJavaPredicate[T](underlying)
  }

  class FromJavaSupplier[T](jf: java.util.function.Supplier[T])
      extends scala.Function0[T] {
    def apply() = jf.get()
  }

  class RichSupplierAsFunction0[T](
      private val underlying: java.util.function.Supplier[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function0[T] =
      new FromJavaSupplier[T](underlying)
  }

  class AsJavaSupplier[T](sf: scala.Function0[T])
      extends java.util.function.Supplier[T] {
    def get() = sf.apply()
  }

  class RichFunction0AsSupplier[T](private val underlying: scala.Function0[T])
      extends AnyVal {
    @inline def asJava: java.util.function.Supplier[T] =
      new AsJavaSupplier[T](underlying)
  }

  class FromJavaToDoubleBiFunction[T, U](
      jf: java.util.function.ToDoubleBiFunction[T, U]
  ) extends scala.Function2[T, U, Double] {
    def apply(x1: T, x2: U) = jf.applyAsDouble(x1, x2)
  }

  class RichToDoubleBiFunctionAsFunction2[T, U](
      private val underlying: java.util.function.ToDoubleBiFunction[T, U]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, Double] =
      new FromJavaToDoubleBiFunction[T, U](underlying)
  }

  class AsJavaToDoubleBiFunction[T, U](sf: scala.Function2[T, U, Double])
      extends java.util.function.ToDoubleBiFunction[T, U] {
    def applyAsDouble(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsToDoubleBiFunction[T, U](
      private val underlying: scala.Function2[T, U, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToDoubleBiFunction[T, U] =
      new AsJavaToDoubleBiFunction[T, U](underlying)
  }

  class FromJavaToDoubleFunction[T](jf: java.util.function.ToDoubleFunction[T])
      extends scala.Function1[T, Double] {
    def apply(x1: T) = jf.applyAsDouble(x1)
  }

  class RichToDoubleFunctionAsFunction1[T](
      private val underlying: java.util.function.ToDoubleFunction[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, Double] =
      new FromJavaToDoubleFunction[T](underlying)
  }

  class AsJavaToDoubleFunction[T](sf: scala.Function1[T, Double])
      extends java.util.function.ToDoubleFunction[T] {
    def applyAsDouble(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsToDoubleFunction[T](
      private val underlying: scala.Function1[T, Double]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToDoubleFunction[T] =
      new AsJavaToDoubleFunction[T](underlying)
  }

  class FromJavaToIntBiFunction[T, U](
      jf: java.util.function.ToIntBiFunction[T, U]
  ) extends scala.Function2[T, U, Int] {
    def apply(x1: T, x2: U) = jf.applyAsInt(x1, x2)
  }

  class RichToIntBiFunctionAsFunction2[T, U](
      private val underlying: java.util.function.ToIntBiFunction[T, U]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, Int] =
      new FromJavaToIntBiFunction[T, U](underlying)
  }

  class AsJavaToIntBiFunction[T, U](sf: scala.Function2[T, U, Int])
      extends java.util.function.ToIntBiFunction[T, U] {
    def applyAsInt(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsToIntBiFunction[T, U](
      private val underlying: scala.Function2[T, U, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToIntBiFunction[T, U] =
      new AsJavaToIntBiFunction[T, U](underlying)
  }

  class FromJavaToIntFunction[T](jf: java.util.function.ToIntFunction[T])
      extends scala.Function1[T, Int] {
    def apply(x1: T) = jf.applyAsInt(x1)
  }

  class RichToIntFunctionAsFunction1[T](
      private val underlying: java.util.function.ToIntFunction[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, Int] =
      new FromJavaToIntFunction[T](underlying)
  }

  class AsJavaToIntFunction[T](sf: scala.Function1[T, Int])
      extends java.util.function.ToIntFunction[T] {
    def applyAsInt(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsToIntFunction[T](
      private val underlying: scala.Function1[T, Int]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToIntFunction[T] =
      new AsJavaToIntFunction[T](underlying)
  }

  class FromJavaToLongBiFunction[T, U](
      jf: java.util.function.ToLongBiFunction[T, U]
  ) extends scala.Function2[T, U, Long] {
    def apply(x1: T, x2: U) = jf.applyAsLong(x1, x2)
  }

  class RichToLongBiFunctionAsFunction2[T, U](
      private val underlying: java.util.function.ToLongBiFunction[T, U]
  ) extends AnyVal {
    @inline def asScala: scala.Function2[T, U, Long] =
      new FromJavaToLongBiFunction[T, U](underlying)
  }

  class AsJavaToLongBiFunction[T, U](sf: scala.Function2[T, U, Long])
      extends java.util.function.ToLongBiFunction[T, U] {
    def applyAsLong(x1: T, x2: U) = sf.apply(x1, x2)
  }

  class RichFunction2AsToLongBiFunction[T, U](
      private val underlying: scala.Function2[T, U, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToLongBiFunction[T, U] =
      new AsJavaToLongBiFunction[T, U](underlying)
  }

  class FromJavaToLongFunction[T](jf: java.util.function.ToLongFunction[T])
      extends scala.Function1[T, Long] {
    def apply(x1: T) = jf.applyAsLong(x1)
  }

  class RichToLongFunctionAsFunction1[T](
      private val underlying: java.util.function.ToLongFunction[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, Long] =
      new FromJavaToLongFunction[T](underlying)
  }

  class AsJavaToLongFunction[T](sf: scala.Function1[T, Long])
      extends java.util.function.ToLongFunction[T] {
    def applyAsLong(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsToLongFunction[T](
      private val underlying: scala.Function1[T, Long]
  ) extends AnyVal {
    @inline def asJava: java.util.function.ToLongFunction[T] =
      new AsJavaToLongFunction[T](underlying)
  }

  class FromJavaUnaryOperator[T](jf: java.util.function.UnaryOperator[T])
      extends scala.Function1[T, T] {
    def apply(x1: T) = jf.apply(x1)
  }

  class RichUnaryOperatorAsFunction1[T](
      private val underlying: java.util.function.UnaryOperator[T]
  ) extends AnyVal {
    @inline def asScala: scala.Function1[T, T] =
      new FromJavaUnaryOperator[T](underlying)
  }

  class AsJavaUnaryOperator[T](sf: scala.Function1[T, T])
      extends java.util.function.UnaryOperator[T] {
    def apply(x1: T) = sf.apply(x1)
  }

  class RichFunction1AsUnaryOperator[T](
      private val underlying: scala.Function1[T, T]
  ) extends AnyVal {
    @inline def asJava: java.util.function.UnaryOperator[T] =
      new AsJavaUnaryOperator[T](underlying)
  }

}

trait Priority3FunctionConverters {
  import functionConverterImpls._

  @inline implicit def enrichAsJavaBiFunction[T, U, R](
      sf: scala.Function2[T, U, R]
  ): RichFunction2AsBiFunction[T, U, R] =
    new RichFunction2AsBiFunction[T, U, R](sf)

}

trait Priority2FunctionConverters extends Priority3FunctionConverters {
  import functionConverterImpls._

  @inline implicit def enrichAsJavaBiConsumer[T, U](
      sf: scala.Function2[T, U, Unit]
  ): RichFunction2AsBiConsumer[T, U] = new RichFunction2AsBiConsumer[T, U](sf)

  @inline implicit def enrichAsJavaBiPredicate[T, U](
      sf: scala.Function2[T, U, Boolean]
  ): RichFunction2AsBiPredicate[T, U] = new RichFunction2AsBiPredicate[T, U](sf)

  @inline implicit def enrichAsJavaFunction[T, R](
      sf: scala.Function1[T, R]
  ): RichFunction1AsFunction[T, R] = new RichFunction1AsFunction[T, R](sf)

  @inline implicit def enrichAsJavaToDoubleBiFunction[T, U](
      sf: scala.Function2[T, U, Double]
  ): RichFunction2AsToDoubleBiFunction[T, U] =
    new RichFunction2AsToDoubleBiFunction[T, U](sf)

  @inline implicit def enrichAsJavaToIntBiFunction[T, U](
      sf: scala.Function2[T, U, Int]
  ): RichFunction2AsToIntBiFunction[T, U] =
    new RichFunction2AsToIntBiFunction[T, U](sf)

  @inline implicit def enrichAsJavaToLongBiFunction[T, U](
      sf: scala.Function2[T, U, Long]
  ): RichFunction2AsToLongBiFunction[T, U] =
    new RichFunction2AsToLongBiFunction[T, U](sf)

}

trait Priority1FunctionConverters extends Priority2FunctionConverters {
  import functionConverterImpls._

  @inline implicit def enrichAsJavaBinaryOperator[T, A1, A2](
      sf: scala.Function2[T, A1, A2]
  )(implicit
      evA1: =:=[A1, T],
      evA2: =:=[A2, T]
  ): RichFunction2AsBinaryOperator[T] = new RichFunction2AsBinaryOperator[T](
    sf.asInstanceOf[scala.Function2[T, T, T]] // scalafix:ok
  )

  @inline implicit def enrichAsJavaConsumer[T](
      sf: scala.Function1[T, Unit]
  ): RichFunction1AsConsumer[T] = new RichFunction1AsConsumer[T](sf)

  @inline implicit def enrichAsJavaDoubleFunction[A0, R](
      sf: scala.Function1[A0, R]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoubleFunction[R] =
    new RichFunction1AsDoubleFunction[R](
      sf.asInstanceOf[scala.Function1[Double, R]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaIntFunction[A0, R](
      sf: scala.Function1[A0, R]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntFunction[R] =
    new RichFunction1AsIntFunction[R](
      sf.asInstanceOf[scala.Function1[Int, R]]
    ) // scalafix:ok

  @inline implicit def enrichAsJavaLongFunction[A0, R](
      sf: scala.Function1[A0, R]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongFunction[R] =
    new RichFunction1AsLongFunction[R](
      sf.asInstanceOf[scala.Function1[Long, R]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaObjDoubleConsumer[T, A1](
      sf: scala.Function2[T, A1, Unit]
  )(implicit evA1: =:=[A1, Double]): RichFunction2AsObjDoubleConsumer[T] =
    new RichFunction2AsObjDoubleConsumer[T](
      sf.asInstanceOf[scala.Function2[T, Double, Unit]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaObjIntConsumer[T, A1](
      sf: scala.Function2[T, A1, Unit]
  )(implicit evA1: =:=[A1, Int]): RichFunction2AsObjIntConsumer[T] =
    new RichFunction2AsObjIntConsumer[T](
      sf.asInstanceOf[scala.Function2[T, Int, Unit]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaObjLongConsumer[T, A1](
      sf: scala.Function2[T, A1, Unit]
  )(implicit evA1: =:=[A1, Long]): RichFunction2AsObjLongConsumer[T] =
    new RichFunction2AsObjLongConsumer[T](
      sf.asInstanceOf[scala.Function2[T, Long, Unit]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaPredicate[T](
      sf: scala.Function1[T, Boolean]
  ): RichFunction1AsPredicate[T] = new RichFunction1AsPredicate[T](sf)

  @inline implicit def enrichAsJavaSupplier[T](
      sf: scala.Function0[T]
  ): RichFunction0AsSupplier[T] = new RichFunction0AsSupplier[T](sf)

  @inline implicit def enrichAsJavaToDoubleFunction[T](
      sf: scala.Function1[T, Double]
  ): RichFunction1AsToDoubleFunction[T] =
    new RichFunction1AsToDoubleFunction[T](sf)

  @inline implicit def enrichAsJavaToIntFunction[T](
      sf: scala.Function1[T, Int]
  ): RichFunction1AsToIntFunction[T] = new RichFunction1AsToIntFunction[T](sf)

  @inline implicit def enrichAsJavaToLongFunction[T](
      sf: scala.Function1[T, Long]
  ): RichFunction1AsToLongFunction[T] = new RichFunction1AsToLongFunction[T](sf)

  @inline implicit def enrichAsJavaUnaryOperator[T, A1](
      sf: scala.Function1[T, A1]
  )(implicit evA1: =:=[A1, T]): RichFunction1AsUnaryOperator[T] =
    new RichFunction1AsUnaryOperator[T](
      sf.asInstanceOf[scala.Function1[T, T]]
    ) // scalafix:ok

}

object FunctionConverters extends Priority1FunctionConverters {
  import functionConverterImpls._
  @inline def asScalaFromBiConsumer[T, U](
      jf: java.util.function.BiConsumer[T, U]
  ): scala.Function2[T, U, Unit] = new FromJavaBiConsumer[T, U](jf)

  @inline def asJavaBiConsumer[T, U](
      sf: scala.Function2[T, U, Unit]
  ): java.util.function.BiConsumer[T, U] = new AsJavaBiConsumer[T, U](sf)

  @inline def asScalaFromBiFunction[T, U, R](
      jf: java.util.function.BiFunction[T, U, R]
  ): scala.Function2[T, U, R] = new FromJavaBiFunction[T, U, R](jf)

  @inline def asJavaBiFunction[T, U, R](
      sf: scala.Function2[T, U, R]
  ): java.util.function.BiFunction[T, U, R] = new AsJavaBiFunction[T, U, R](sf)

  @inline def asScalaFromBiPredicate[T, U](
      jf: java.util.function.BiPredicate[T, U]
  ): scala.Function2[T, U, Boolean] = new FromJavaBiPredicate[T, U](jf)

  @inline def asJavaBiPredicate[T, U](
      sf: scala.Function2[T, U, Boolean]
  ): java.util.function.BiPredicate[T, U] = new AsJavaBiPredicate[T, U](sf)

  @inline def asScalaFromBinaryOperator[T](
      jf: java.util.function.BinaryOperator[T]
  ): scala.Function2[T, T, T] = new FromJavaBinaryOperator[T](jf)

  @inline def asJavaBinaryOperator[T](
      sf: scala.Function2[T, T, T]
  ): java.util.function.BinaryOperator[T] = new AsJavaBinaryOperator[T](sf)

  @inline def asScalaFromBooleanSupplier(
      jf: java.util.function.BooleanSupplier
  ): scala.Function0[Boolean] = new FromJavaBooleanSupplier(jf)

  @inline def asJavaBooleanSupplier(
      sf: scala.Function0[Boolean]
  ): java.util.function.BooleanSupplier = new AsJavaBooleanSupplier(sf)

  @inline def asScalaFromConsumer[T](
      jf: java.util.function.Consumer[T]
  ): scala.Function1[T, Unit] = new FromJavaConsumer[T](jf)

  @inline def asJavaConsumer[T](
      sf: scala.Function1[T, Unit]
  ): java.util.function.Consumer[T] = new AsJavaConsumer[T](sf)

  @inline def asScalaFromDoubleBinaryOperator(
      jf: java.util.function.DoubleBinaryOperator
  ): scala.Function2[Double, Double, Double] = new FromJavaDoubleBinaryOperator(
    jf
  )

  @inline def asJavaDoubleBinaryOperator(
      sf: scala.Function2[Double, Double, Double]
  ): java.util.function.DoubleBinaryOperator = new AsJavaDoubleBinaryOperator(
    sf
  )

  @inline def asScalaFromDoubleConsumer(
      jf: java.util.function.DoubleConsumer
  ): scala.Function1[Double, Unit] = new FromJavaDoubleConsumer(jf)

  @inline def asJavaDoubleConsumer(
      sf: scala.Function1[Double, Unit]
  ): java.util.function.DoubleConsumer = new AsJavaDoubleConsumer(sf)

  @inline def asScalaFromDoubleFunction[R](
      jf: java.util.function.DoubleFunction[R]
  ): scala.Function1[Double, R] = new FromJavaDoubleFunction[R](jf)

  @inline def asJavaDoubleFunction[R](
      sf: scala.Function1[Double, R]
  ): java.util.function.DoubleFunction[R] = new AsJavaDoubleFunction[R](sf)

  @inline def asScalaFromDoublePredicate(
      jf: java.util.function.DoublePredicate
  ): scala.Function1[Double, Boolean] = new FromJavaDoublePredicate(jf)

  @inline def asJavaDoublePredicate(
      sf: scala.Function1[Double, Boolean]
  ): java.util.function.DoublePredicate = new AsJavaDoublePredicate(sf)

  @inline def asScalaFromDoubleSupplier(
      jf: java.util.function.DoubleSupplier
  ): scala.Function0[Double] = new FromJavaDoubleSupplier(jf)

  @inline def asJavaDoubleSupplier(
      sf: scala.Function0[Double]
  ): java.util.function.DoubleSupplier = new AsJavaDoubleSupplier(sf)

  @inline def asScalaFromDoubleToIntFunction(
      jf: java.util.function.DoubleToIntFunction
  ): scala.Function1[Double, Int] = new FromJavaDoubleToIntFunction(jf)

  @inline def asJavaDoubleToIntFunction(
      sf: scala.Function1[Double, Int]
  ): java.util.function.DoubleToIntFunction = new AsJavaDoubleToIntFunction(sf)

  @inline def asScalaFromDoubleToLongFunction(
      jf: java.util.function.DoubleToLongFunction
  ): scala.Function1[Double, Long] = new FromJavaDoubleToLongFunction(jf)

  @inline def asJavaDoubleToLongFunction(
      sf: scala.Function1[Double, Long]
  ): java.util.function.DoubleToLongFunction = new AsJavaDoubleToLongFunction(
    sf
  )

  @inline def asScalaFromDoubleUnaryOperator(
      jf: java.util.function.DoubleUnaryOperator
  ): scala.Function1[Double, Double] = new FromJavaDoubleUnaryOperator(jf)

  @inline def asJavaDoubleUnaryOperator(
      sf: scala.Function1[Double, Double]
  ): java.util.function.DoubleUnaryOperator = new AsJavaDoubleUnaryOperator(sf)

  @inline def asScalaFromFunction[T, R](
      jf: java.util.function.Function[T, R]
  ): scala.Function1[T, R] = new FromJavaFunction[T, R](jf)

  @inline def asJavaFunction[T, R](
      sf: scala.Function1[T, R]
  ): java.util.function.Function[T, R] = new AsJavaFunction[T, R](sf)

  @inline def asScalaFromIntBinaryOperator(
      jf: java.util.function.IntBinaryOperator
  ): scala.Function2[Int, Int, Int] = new FromJavaIntBinaryOperator(jf)

  @inline def asJavaIntBinaryOperator(
      sf: scala.Function2[Int, Int, Int]
  ): java.util.function.IntBinaryOperator = new AsJavaIntBinaryOperator(sf)

  @inline def asScalaFromIntConsumer(
      jf: java.util.function.IntConsumer
  ): scala.Function1[Int, Unit] = new FromJavaIntConsumer(jf)

  @inline def asJavaIntConsumer(
      sf: scala.Function1[Int, Unit]
  ): java.util.function.IntConsumer = new AsJavaIntConsumer(sf)

  @inline def asScalaFromIntFunction[R](
      jf: java.util.function.IntFunction[R]
  ): scala.Function1[Int, R] = new FromJavaIntFunction[R](jf)

  @inline def asJavaIntFunction[R](
      sf: scala.Function1[Int, R]
  ): java.util.function.IntFunction[R] = new AsJavaIntFunction[R](sf)

  @inline def asScalaFromIntPredicate(
      jf: java.util.function.IntPredicate
  ): scala.Function1[Int, Boolean] = new FromJavaIntPredicate(jf)

  @inline def asJavaIntPredicate(
      sf: scala.Function1[Int, Boolean]
  ): java.util.function.IntPredicate = new AsJavaIntPredicate(sf)

  @inline def asScalaFromIntSupplier(
      jf: java.util.function.IntSupplier
  ): scala.Function0[Int] = new FromJavaIntSupplier(jf)

  @inline def asJavaIntSupplier(
      sf: scala.Function0[Int]
  ): java.util.function.IntSupplier = new AsJavaIntSupplier(sf)

  @inline def asScalaFromIntToDoubleFunction(
      jf: java.util.function.IntToDoubleFunction
  ): scala.Function1[Int, Double] = new FromJavaIntToDoubleFunction(jf)

  @inline def asJavaIntToDoubleFunction(
      sf: scala.Function1[Int, Double]
  ): java.util.function.IntToDoubleFunction = new AsJavaIntToDoubleFunction(sf)

  @inline def asScalaFromIntToLongFunction(
      jf: java.util.function.IntToLongFunction
  ): scala.Function1[Int, Long] = new FromJavaIntToLongFunction(jf)

  @inline def asJavaIntToLongFunction(
      sf: scala.Function1[Int, Long]
  ): java.util.function.IntToLongFunction = new AsJavaIntToLongFunction(sf)

  @inline def asScalaFromIntUnaryOperator(
      jf: java.util.function.IntUnaryOperator
  ): scala.Function1[Int, Int] = new FromJavaIntUnaryOperator(jf)

  @inline def asJavaIntUnaryOperator(
      sf: scala.Function1[Int, Int]
  ): java.util.function.IntUnaryOperator = new AsJavaIntUnaryOperator(sf)

  @inline def asScalaFromLongBinaryOperator(
      jf: java.util.function.LongBinaryOperator
  ): scala.Function2[Long, Long, Long] = new FromJavaLongBinaryOperator(jf)

  @inline def asJavaLongBinaryOperator(
      sf: scala.Function2[Long, Long, Long]
  ): java.util.function.LongBinaryOperator = new AsJavaLongBinaryOperator(sf)

  @inline def asScalaFromLongConsumer(
      jf: java.util.function.LongConsumer
  ): scala.Function1[Long, Unit] = new FromJavaLongConsumer(jf)

  @inline def asJavaLongConsumer(
      sf: scala.Function1[Long, Unit]
  ): java.util.function.LongConsumer = new AsJavaLongConsumer(sf)

  @inline def asScalaFromLongFunction[R](
      jf: java.util.function.LongFunction[R]
  ): scala.Function1[Long, R] = new FromJavaLongFunction[R](jf)

  @inline def asJavaLongFunction[R](
      sf: scala.Function1[Long, R]
  ): java.util.function.LongFunction[R] = new AsJavaLongFunction[R](sf)

  @inline def asScalaFromLongPredicate(
      jf: java.util.function.LongPredicate
  ): scala.Function1[Long, Boolean] = new FromJavaLongPredicate(jf)

  @inline def asJavaLongPredicate(
      sf: scala.Function1[Long, Boolean]
  ): java.util.function.LongPredicate = new AsJavaLongPredicate(sf)

  @inline def asScalaFromLongSupplier(
      jf: java.util.function.LongSupplier
  ): scala.Function0[Long] = new FromJavaLongSupplier(jf)

  @inline def asJavaLongSupplier(
      sf: scala.Function0[Long]
  ): java.util.function.LongSupplier = new AsJavaLongSupplier(sf)

  @inline def asScalaFromLongToDoubleFunction(
      jf: java.util.function.LongToDoubleFunction
  ): scala.Function1[Long, Double] = new FromJavaLongToDoubleFunction(jf)

  @inline def asJavaLongToDoubleFunction(
      sf: scala.Function1[Long, Double]
  ): java.util.function.LongToDoubleFunction = new AsJavaLongToDoubleFunction(
    sf
  )

  @inline def asScalaFromLongToIntFunction(
      jf: java.util.function.LongToIntFunction
  ): scala.Function1[Long, Int] = new FromJavaLongToIntFunction(jf)

  @inline def asJavaLongToIntFunction(
      sf: scala.Function1[Long, Int]
  ): java.util.function.LongToIntFunction = new AsJavaLongToIntFunction(sf)

  @inline def asScalaFromLongUnaryOperator(
      jf: java.util.function.LongUnaryOperator
  ): scala.Function1[Long, Long] = new FromJavaLongUnaryOperator(jf)

  @inline def asJavaLongUnaryOperator(
      sf: scala.Function1[Long, Long]
  ): java.util.function.LongUnaryOperator = new AsJavaLongUnaryOperator(sf)

  @inline def asScalaFromObjDoubleConsumer[T](
      jf: java.util.function.ObjDoubleConsumer[T]
  ): scala.Function2[T, Double, Unit] = new FromJavaObjDoubleConsumer[T](jf)

  @inline def asJavaObjDoubleConsumer[T](
      sf: scala.Function2[T, Double, Unit]
  ): java.util.function.ObjDoubleConsumer[T] =
    new AsJavaObjDoubleConsumer[T](sf)

  @inline def asScalaFromObjIntConsumer[T](
      jf: java.util.function.ObjIntConsumer[T]
  ): scala.Function2[T, Int, Unit] = new FromJavaObjIntConsumer[T](jf)

  @inline def asJavaObjIntConsumer[T](
      sf: scala.Function2[T, Int, Unit]
  ): java.util.function.ObjIntConsumer[T] = new AsJavaObjIntConsumer[T](sf)

  @inline def asScalaFromObjLongConsumer[T](
      jf: java.util.function.ObjLongConsumer[T]
  ): scala.Function2[T, Long, Unit] = new FromJavaObjLongConsumer[T](jf)

  @inline def asJavaObjLongConsumer[T](
      sf: scala.Function2[T, Long, Unit]
  ): java.util.function.ObjLongConsumer[T] = new AsJavaObjLongConsumer[T](sf)

  @inline def asScalaFromPredicate[T](
      jf: java.util.function.Predicate[T]
  ): scala.Function1[T, Boolean] = new FromJavaPredicate[T](jf)

  @inline def asJavaPredicate[T](
      sf: scala.Function1[T, Boolean]
  ): java.util.function.Predicate[T] = new AsJavaPredicate[T](sf)

  @inline def asScalaFromSupplier[T](
      jf: java.util.function.Supplier[T]
  ): scala.Function0[T] = new FromJavaSupplier[T](jf)

  @inline def asJavaSupplier[T](
      sf: scala.Function0[T]
  ): java.util.function.Supplier[T] = new AsJavaSupplier[T](sf)

  @inline def asScalaFromToDoubleBiFunction[T, U](
      jf: java.util.function.ToDoubleBiFunction[T, U]
  ): scala.Function2[T, U, Double] = new FromJavaToDoubleBiFunction[T, U](jf)

  @inline def asJavaToDoubleBiFunction[T, U](
      sf: scala.Function2[T, U, Double]
  ): java.util.function.ToDoubleBiFunction[T, U] =
    new AsJavaToDoubleBiFunction[T, U](sf)

  @inline def asScalaFromToDoubleFunction[T](
      jf: java.util.function.ToDoubleFunction[T]
  ): scala.Function1[T, Double] = new FromJavaToDoubleFunction[T](jf)

  @inline def asJavaToDoubleFunction[T](
      sf: scala.Function1[T, Double]
  ): java.util.function.ToDoubleFunction[T] = new AsJavaToDoubleFunction[T](sf)

  @inline def asScalaFromToIntBiFunction[T, U](
      jf: java.util.function.ToIntBiFunction[T, U]
  ): scala.Function2[T, U, Int] = new FromJavaToIntBiFunction[T, U](jf)

  @inline def asJavaToIntBiFunction[T, U](
      sf: scala.Function2[T, U, Int]
  ): java.util.function.ToIntBiFunction[T, U] =
    new AsJavaToIntBiFunction[T, U](sf)

  @inline def asScalaFromToIntFunction[T](
      jf: java.util.function.ToIntFunction[T]
  ): scala.Function1[T, Int] = new FromJavaToIntFunction[T](jf)

  @inline def asJavaToIntFunction[T](
      sf: scala.Function1[T, Int]
  ): java.util.function.ToIntFunction[T] = new AsJavaToIntFunction[T](sf)

  @inline def asScalaFromToLongBiFunction[T, U](
      jf: java.util.function.ToLongBiFunction[T, U]
  ): scala.Function2[T, U, Long] = new FromJavaToLongBiFunction[T, U](jf)

  @inline def asJavaToLongBiFunction[T, U](
      sf: scala.Function2[T, U, Long]
  ): java.util.function.ToLongBiFunction[T, U] =
    new AsJavaToLongBiFunction[T, U](sf)

  @inline def asScalaFromToLongFunction[T](
      jf: java.util.function.ToLongFunction[T]
  ): scala.Function1[T, Long] = new FromJavaToLongFunction[T](jf)

  @inline def asJavaToLongFunction[T](
      sf: scala.Function1[T, Long]
  ): java.util.function.ToLongFunction[T] = new AsJavaToLongFunction[T](sf)

  @inline def asScalaFromUnaryOperator[T](
      jf: java.util.function.UnaryOperator[T]
  ): scala.Function1[T, T] = new FromJavaUnaryOperator[T](jf)

  @inline def asJavaUnaryOperator[T](
      sf: scala.Function1[T, T]
  ): java.util.function.UnaryOperator[T] = new AsJavaUnaryOperator[T](sf)
  @inline implicit def enrichAsJavaBooleanSupplier(
      sf: scala.Function0[Boolean]
  ): RichFunction0AsBooleanSupplier = new RichFunction0AsBooleanSupplier(sf)

  @inline implicit def enrichAsJavaDoubleBinaryOperator[A0, A1](
      sf: scala.Function2[A0, A1, Double]
  )(implicit
      evA0: =:=[A0, Double],
      evA1: =:=[A1, Double]
  ): RichFunction2AsDoubleBinaryOperator =
    new RichFunction2AsDoubleBinaryOperator(
      sf.asInstanceOf[scala.Function2[Double, Double, Double]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaDoubleConsumer[A0](
      sf: scala.Function1[A0, Unit]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoubleConsumer =
    new RichFunction1AsDoubleConsumer(
      sf.asInstanceOf[scala.Function1[Double, Unit]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaDoublePredicate[A0](
      sf: scala.Function1[A0, Boolean]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoublePredicate =
    new RichFunction1AsDoublePredicate(
      sf.asInstanceOf[scala.Function1[Double, Boolean]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaDoubleSupplier(
      sf: scala.Function0[Double]
  ): RichFunction0AsDoubleSupplier = new RichFunction0AsDoubleSupplier(sf)

  @inline implicit def enrichAsJavaDoubleToIntFunction[A0](
      sf: scala.Function1[A0, Int]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoubleToIntFunction =
    new RichFunction1AsDoubleToIntFunction(
      sf.asInstanceOf[scala.Function1[Double, Int]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaDoubleToLongFunction[A0](
      sf: scala.Function1[A0, Long]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoubleToLongFunction =
    new RichFunction1AsDoubleToLongFunction(
      sf.asInstanceOf[scala.Function1[Double, Long]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaDoubleUnaryOperator[A0](
      sf: scala.Function1[A0, Double]
  )(implicit evA0: =:=[A0, Double]): RichFunction1AsDoubleUnaryOperator =
    new RichFunction1AsDoubleUnaryOperator(
      sf.asInstanceOf[scala.Function1[Double, Double]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaIntBinaryOperator[A0, A1](
      sf: scala.Function2[A0, A1, Int]
  )(implicit
      evA0: =:=[A0, Int],
      evA1: =:=[A1, Int]
  ): RichFunction2AsIntBinaryOperator = new RichFunction2AsIntBinaryOperator(
    sf.asInstanceOf[scala.Function2[Int, Int, Int]] // scalafix:ok
  )

  @inline implicit def enrichAsJavaIntConsumer[A0](
      sf: scala.Function1[A0, Unit]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntConsumer =
    new RichFunction1AsIntConsumer(
      sf.asInstanceOf[scala.Function1[Int, Unit]]
    ) // scalafix:ok

  @inline implicit def enrichAsJavaIntPredicate[A0](
      sf: scala.Function1[A0, Boolean]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntPredicate =
    new RichFunction1AsIntPredicate(
      sf.asInstanceOf[scala.Function1[Int, Boolean]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaIntSupplier(
      sf: scala.Function0[Int]
  ): RichFunction0AsIntSupplier = new RichFunction0AsIntSupplier(sf)

  @inline implicit def enrichAsJavaIntToDoubleFunction[A0](
      sf: scala.Function1[A0, Double]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntToDoubleFunction =
    new RichFunction1AsIntToDoubleFunction(
      sf.asInstanceOf[scala.Function1[Int, Double]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaIntToLongFunction[A0](
      sf: scala.Function1[A0, Long]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntToLongFunction =
    new RichFunction1AsIntToLongFunction(
      sf.asInstanceOf[scala.Function1[Int, Long]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaIntUnaryOperator[A0](
      sf: scala.Function1[A0, Int]
  )(implicit evA0: =:=[A0, Int]): RichFunction1AsIntUnaryOperator =
    new RichFunction1AsIntUnaryOperator(
      sf.asInstanceOf[scala.Function1[Int, Int]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaLongBinaryOperator[A0, A1](
      sf: scala.Function2[A0, A1, Long]
  )(implicit
      evA0: =:=[A0, Long],
      evA1: =:=[A1, Long]
  ): RichFunction2AsLongBinaryOperator = new RichFunction2AsLongBinaryOperator(
    sf.asInstanceOf[scala.Function2[Long, Long, Long]] // scalafix:ok
  )

  @inline implicit def enrichAsJavaLongConsumer[A0](
      sf: scala.Function1[A0, Unit]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongConsumer =
    new RichFunction1AsLongConsumer(
      sf.asInstanceOf[scala.Function1[Long, Unit]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaLongPredicate[A0](
      sf: scala.Function1[A0, Boolean]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongPredicate =
    new RichFunction1AsLongPredicate(
      sf.asInstanceOf[scala.Function1[Long, Boolean]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaLongSupplier(
      sf: scala.Function0[Long]
  ): RichFunction0AsLongSupplier = new RichFunction0AsLongSupplier(sf)

  @inline implicit def enrichAsJavaLongToDoubleFunction[A0](
      sf: scala.Function1[A0, Double]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongToDoubleFunction =
    new RichFunction1AsLongToDoubleFunction(
      sf.asInstanceOf[scala.Function1[Long, Double]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaLongToIntFunction[A0](
      sf: scala.Function1[A0, Int]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongToIntFunction =
    new RichFunction1AsLongToIntFunction(
      sf.asInstanceOf[scala.Function1[Long, Int]] // scalafix:ok
    )

  @inline implicit def enrichAsJavaLongUnaryOperator[A0](
      sf: scala.Function1[A0, Long]
  )(implicit evA0: =:=[A0, Long]): RichFunction1AsLongUnaryOperator =
    new RichFunction1AsLongUnaryOperator(
      sf.asInstanceOf[scala.Function1[Long, Long]] // scalafix:ok
    )
  @inline implicit def enrichAsScalaFromBiConsumer[T, U](
      jf: java.util.function.BiConsumer[T, U]
  ): RichBiConsumerAsFunction2[T, U] = new RichBiConsumerAsFunction2[T, U](jf)

  @inline implicit def enrichAsScalaFromBiFunction[T, U, R](
      jf: java.util.function.BiFunction[T, U, R]
  ): RichBiFunctionAsFunction2[T, U, R] =
    new RichBiFunctionAsFunction2[T, U, R](jf)

  @inline implicit def enrichAsScalaFromBiPredicate[T, U](
      jf: java.util.function.BiPredicate[T, U]
  ): RichBiPredicateAsFunction2[T, U] = new RichBiPredicateAsFunction2[T, U](jf)

  @inline implicit def enrichAsScalaFromBinaryOperator[T](
      jf: java.util.function.BinaryOperator[T]
  ): RichBinaryOperatorAsFunction2[T] = new RichBinaryOperatorAsFunction2[T](jf)

  @inline implicit def enrichAsScalaFromBooleanSupplier(
      jf: java.util.function.BooleanSupplier
  ): RichBooleanSupplierAsFunction0 = new RichBooleanSupplierAsFunction0(jf)

  @inline implicit def enrichAsScalaFromConsumer[T](
      jf: java.util.function.Consumer[T]
  ): RichConsumerAsFunction1[T] = new RichConsumerAsFunction1[T](jf)

  @inline implicit def enrichAsScalaFromDoubleBinaryOperator(
      jf: java.util.function.DoubleBinaryOperator
  ): RichDoubleBinaryOperatorAsFunction2 =
    new RichDoubleBinaryOperatorAsFunction2(jf)

  @inline implicit def enrichAsScalaFromDoubleConsumer(
      jf: java.util.function.DoubleConsumer
  ): RichDoubleConsumerAsFunction1 = new RichDoubleConsumerAsFunction1(jf)

  @inline implicit def enrichAsScalaFromDoubleFunction[R](
      jf: java.util.function.DoubleFunction[R]
  ): RichDoubleFunctionAsFunction1[R] = new RichDoubleFunctionAsFunction1[R](jf)

  @inline implicit def enrichAsScalaFromDoublePredicate(
      jf: java.util.function.DoublePredicate
  ): RichDoublePredicateAsFunction1 = new RichDoublePredicateAsFunction1(jf)

  @inline implicit def enrichAsScalaFromDoubleSupplier(
      jf: java.util.function.DoubleSupplier
  ): RichDoubleSupplierAsFunction0 = new RichDoubleSupplierAsFunction0(jf)

  @inline implicit def enrichAsScalaFromDoubleToIntFunction(
      jf: java.util.function.DoubleToIntFunction
  ): RichDoubleToIntFunctionAsFunction1 =
    new RichDoubleToIntFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromDoubleToLongFunction(
      jf: java.util.function.DoubleToLongFunction
  ): RichDoubleToLongFunctionAsFunction1 =
    new RichDoubleToLongFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromDoubleUnaryOperator(
      jf: java.util.function.DoubleUnaryOperator
  ): RichDoubleUnaryOperatorAsFunction1 =
    new RichDoubleUnaryOperatorAsFunction1(jf)

  @inline implicit def enrichAsScalaFromFunction[T, R](
      jf: java.util.function.Function[T, R]
  ): RichFunctionAsFunction1[T, R] = new RichFunctionAsFunction1[T, R](jf)

  @inline implicit def enrichAsScalaFromIntBinaryOperator(
      jf: java.util.function.IntBinaryOperator
  ): RichIntBinaryOperatorAsFunction2 = new RichIntBinaryOperatorAsFunction2(jf)

  @inline implicit def enrichAsScalaFromIntConsumer(
      jf: java.util.function.IntConsumer
  ): RichIntConsumerAsFunction1 = new RichIntConsumerAsFunction1(jf)

  @inline implicit def enrichAsScalaFromIntFunction[R](
      jf: java.util.function.IntFunction[R]
  ): RichIntFunctionAsFunction1[R] = new RichIntFunctionAsFunction1[R](jf)

  @inline implicit def enrichAsScalaFromIntPredicate(
      jf: java.util.function.IntPredicate
  ): RichIntPredicateAsFunction1 = new RichIntPredicateAsFunction1(jf)

  @inline implicit def enrichAsScalaFromIntSupplier(
      jf: java.util.function.IntSupplier
  ): RichIntSupplierAsFunction0 = new RichIntSupplierAsFunction0(jf)

  @inline implicit def enrichAsScalaFromIntToDoubleFunction(
      jf: java.util.function.IntToDoubleFunction
  ): RichIntToDoubleFunctionAsFunction1 =
    new RichIntToDoubleFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromIntToLongFunction(
      jf: java.util.function.IntToLongFunction
  ): RichIntToLongFunctionAsFunction1 = new RichIntToLongFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromIntUnaryOperator(
      jf: java.util.function.IntUnaryOperator
  ): RichIntUnaryOperatorAsFunction1 = new RichIntUnaryOperatorAsFunction1(jf)

  @inline implicit def enrichAsScalaFromLongBinaryOperator(
      jf: java.util.function.LongBinaryOperator
  ): RichLongBinaryOperatorAsFunction2 = new RichLongBinaryOperatorAsFunction2(
    jf
  )

  @inline implicit def enrichAsScalaFromLongConsumer(
      jf: java.util.function.LongConsumer
  ): RichLongConsumerAsFunction1 = new RichLongConsumerAsFunction1(jf)

  @inline implicit def enrichAsScalaFromLongFunction[R](
      jf: java.util.function.LongFunction[R]
  ): RichLongFunctionAsFunction1[R] = new RichLongFunctionAsFunction1[R](jf)

  @inline implicit def enrichAsScalaFromLongPredicate(
      jf: java.util.function.LongPredicate
  ): RichLongPredicateAsFunction1 = new RichLongPredicateAsFunction1(jf)

  @inline implicit def enrichAsScalaFromLongSupplier(
      jf: java.util.function.LongSupplier
  ): RichLongSupplierAsFunction0 = new RichLongSupplierAsFunction0(jf)

  @inline implicit def enrichAsScalaFromLongToDoubleFunction(
      jf: java.util.function.LongToDoubleFunction
  ): RichLongToDoubleFunctionAsFunction1 =
    new RichLongToDoubleFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromLongToIntFunction(
      jf: java.util.function.LongToIntFunction
  ): RichLongToIntFunctionAsFunction1 = new RichLongToIntFunctionAsFunction1(jf)

  @inline implicit def enrichAsScalaFromLongUnaryOperator(
      jf: java.util.function.LongUnaryOperator
  ): RichLongUnaryOperatorAsFunction1 = new RichLongUnaryOperatorAsFunction1(jf)

  @inline implicit def enrichAsScalaFromObjDoubleConsumer[T](
      jf: java.util.function.ObjDoubleConsumer[T]
  ): RichObjDoubleConsumerAsFunction2[T] =
    new RichObjDoubleConsumerAsFunction2[T](jf)

  @inline implicit def enrichAsScalaFromObjIntConsumer[T](
      jf: java.util.function.ObjIntConsumer[T]
  ): RichObjIntConsumerAsFunction2[T] = new RichObjIntConsumerAsFunction2[T](jf)

  @inline implicit def enrichAsScalaFromObjLongConsumer[T](
      jf: java.util.function.ObjLongConsumer[T]
  ): RichObjLongConsumerAsFunction2[T] =
    new RichObjLongConsumerAsFunction2[T](jf)

  @inline implicit def enrichAsScalaFromPredicate[T](
      jf: java.util.function.Predicate[T]
  ): RichPredicateAsFunction1[T] = new RichPredicateAsFunction1[T](jf)

  @inline implicit def enrichAsScalaFromSupplier[T](
      jf: java.util.function.Supplier[T]
  ): RichSupplierAsFunction0[T] = new RichSupplierAsFunction0[T](jf)

  @inline implicit def enrichAsScalaFromToDoubleBiFunction[T, U](
      jf: java.util.function.ToDoubleBiFunction[T, U]
  ): RichToDoubleBiFunctionAsFunction2[T, U] =
    new RichToDoubleBiFunctionAsFunction2[T, U](jf)

  @inline implicit def enrichAsScalaFromToDoubleFunction[T](
      jf: java.util.function.ToDoubleFunction[T]
  ): RichToDoubleFunctionAsFunction1[T] =
    new RichToDoubleFunctionAsFunction1[T](jf)

  @inline implicit def enrichAsScalaFromToIntBiFunction[T, U](
      jf: java.util.function.ToIntBiFunction[T, U]
  ): RichToIntBiFunctionAsFunction2[T, U] =
    new RichToIntBiFunctionAsFunction2[T, U](jf)

  @inline implicit def enrichAsScalaFromToIntFunction[T](
      jf: java.util.function.ToIntFunction[T]
  ): RichToIntFunctionAsFunction1[T] = new RichToIntFunctionAsFunction1[T](jf)

  @inline implicit def enrichAsScalaFromToLongBiFunction[T, U](
      jf: java.util.function.ToLongBiFunction[T, U]
  ): RichToLongBiFunctionAsFunction2[T, U] =
    new RichToLongBiFunctionAsFunction2[T, U](jf)

  @inline implicit def enrichAsScalaFromToLongFunction[T](
      jf: java.util.function.ToLongFunction[T]
  ): RichToLongFunctionAsFunction1[T] = new RichToLongFunctionAsFunction1[T](jf)

  @inline implicit def enrichAsScalaFromUnaryOperator[T](
      jf: java.util.function.UnaryOperator[T]
  ): RichUnaryOperatorAsFunction1[T] = new RichUnaryOperatorAsFunction1[T](jf)
}

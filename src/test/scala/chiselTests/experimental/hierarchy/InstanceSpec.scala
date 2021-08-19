// SPDX-License-Identifier: Apache-2.0

package chiselTests
package experimental.hierarchy

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}

// TODO/Notes
// - In backport, clock/reset are not automatically assigned. I think this is fixed in 3.5
// - CircuitTarget for annotations on the definition are wrong - needs to be fixed.
class InstanceSpec extends ChiselFunSpec with Utils {
  import Annotations._
  import Examples._
  describe("0: Instance instantiation") {
    it("0.0: name of an instance should be correct") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddOne)
        val i0 = Instance(definition)
      }
      check(new Top(), "i0", "AddOne")
    }
    it("0.1: name of an instanceclone should not error") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddTwo)
        val i0 = Instance(definition)
        val i = i0.i0 // This should not error
      }
      check(new Top(), "i0", "AddTwo")
    }
  }
  describe("1: Annotations on instances in same chisel compilation") {
    it("1.0: should work on a single instance, annotating the instance") {
      class Top extends MultiIOModule {
        val definition: Definition[AddOne] = Definition(new AddOne)
        val i0: Instance[AddOne] = Instance(definition)
        mark(i0, "i0")
      }
      check(new Top(), "~Top|Top/i0:AddOne".it, "i0")
    }
    it("1.1: should work on a single instance, annotating an inner wire") {
      class Top extends MultiIOModule {
        val definition: Definition[AddOne] = Definition(new AddOne)
        val i0: Instance[AddOne] = Instance(definition)
        mark(i0.innerWire, "i0.innerWire")
      }
      check(new Top(), "~Top|Top/i0:AddOne>innerWire".rt, "i0.innerWire")
    }
    it("1.2: should work on a two nested instances, annotating the instance") {
      class Top extends MultiIOModule {
        val definition: Definition[AddTwo] = Definition(new AddTwo)
        val i0: Instance[AddTwo] = Instance(definition)
        mark(i0.i0, "i0.i0")
      }
      check(new Top(), "~Top|Top/i0:AddTwo/i0:AddOne".it, "i0.i0")
    }
    it("1.3: should work on a two nested instances, annotating the inner wire") {
      class Top extends MultiIOModule {
        val definition: Definition[AddTwo] = Definition(new AddTwo)
        val i0: Instance[AddTwo] = Instance(definition)
        mark(i0.i0.innerWire, "i0.i0.innerWire")
      }
      check(new Top(), "~Top|Top/i0:AddTwo/i0:AddOne>innerWire".rt, "i0.i0.innerWire")
    }
    it("1.4: should work on a nested module in an instance, annotating the module") {
      class Top extends MultiIOModule {
        val definition: Definition[AddTwoMixedModules] = Definition(new AddTwoMixedModules)
        val i0: Instance[AddTwoMixedModules] = Instance(definition)
        mark(i0.i1, "i0.i1")
      }
      check(new Top(), "~Top|Top/i0:AddTwoMixedModules/i1:AddOne_2".it, "i0.i1")
    }
    it("1.5: should work on an instantiable container, annotating a wire") {
      class Top extends MultiIOModule {
        val definition: Definition[AddOneWithInstantiableWire] = Definition(new AddOneWithInstantiableWire)
        val i0: Instance[AddOneWithInstantiableWire] = Instance(definition)
        mark(i0.wireContainer.innerWire, "i0.innerWire")
      }
      check(new Top(), "~Top|Top/i0:AddOneWithInstantiableWire>innerWire".rt, "i0.innerWire")
    }
    it("1.6: should work on an instantiable container, annotating a module") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddOneWithInstantiableModule)
        val i0 = Instance(definition)
        mark(i0.moduleContainer.i0, "i0.i0")
      }
      check(new Top(), "~Top|Top/i0:AddOneWithInstantiableModule/i0:AddOne".it, "i0.i0")
    }
    it("1.7: should work on an instantiable container, annotating an instance") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddOneWithInstantiableInstance)
        val i0 = Instance(definition)
        mark(i0.instanceContainer.i0, "i0.i0")
      }
      check(new Top(), "~Top|Top/i0:AddOneWithInstantiableInstance/i0:AddOne".it, "i0.i0")
    }
    it("1.8: should work on an instantiable container, annotating an instantiable container's module") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddOneWithInstantiableInstantiable)
        val i0 = Instance(definition)
        mark(i0.containerContainer.container.i0, "i0.i0")
      }
      check(new Top(), "~Top|Top/i0:AddOneWithInstantiableInstantiable/i0:AddOne".it, "i0.i0")
    }
    it("1.9: should work on public member which references public member of another instance") {
      class Top extends MultiIOModule {
        val definition = Definition(new AddOneWithInstantiableInstantiable)
        val i0 = Instance(definition)
        mark(i0.containerContainer.container.i0, "i0.i0")
      }
      check(new Top(), "~Top|Top/i0:AddOneWithInstantiableInstantiable/i0:AddOne".it, "i0.i0")
    }
    it("1.10: should work for targets on definition to have correct circuit name"){
      class Top extends MultiIOModule {
        val definition = Definition(new AddOneWithAnnotation)
        val i0 = Instance(definition)
      }
      check(new Top(), "~Top|AddOneWithAnnotation>innerWire".rt, "innerWire")
    }
  }
  describe("2: Annotations on designs not in the same chisel compilation") {
    it("2.0: should work on an innerWire, marked in a different compilation") {
      val first = elaborateAndGetModule(new AddTwo)
      class Top(x: AddTwo) extends MultiIOModule {
        val parent = Instance(Definition(new ViewerParent(x, false, true)))
      }
      check(new Top(first), "~AddTwo|AddTwo/i0:AddOne>innerWire".rt, "first")
    }
    it("2.1: should work on an innerWire, marked in a different compilation, in instanced instantiable") {
      val first = elaborateAndGetModule(new AddTwo)
      class Top(x: AddTwo) extends MultiIOModule {
        val parent = Instance(Definition(new ViewerParent(x, true, false)))
      }
      check(new Top(first), "~AddTwo|AddTwo/i0:AddOne>innerWire".rt, "second")
    }
    it("2.2: should work on an innerWire, marked in a different compilation, in instanced module") {
      val first = elaborateAndGetModule(new AddTwo)
      class Top(x: AddTwo) extends MultiIOModule {
        val parent = Instance(Definition(new ViewerParent(x, false, false)))
        mark(parent.viewer.x.i0.innerWire, "third")
      }
      check(new Top(first), "~AddTwo|AddTwo/i0:AddOne>innerWire".rt, "third")
    }
  }
  describe("3: @public") {
    it("3.0: should work on multi-vals") {
      class Top() extends MultiIOModule {
        val mv = Instance(Definition(new MultiVal()))
        mark(mv.x, "mv.x")
      }
      check(new Top(), "~Top|Top/mv:MultiVal>x".rt, "mv.x")
    }
    it("3.1: should work on lazy vals") {
      class Top() extends MultiIOModule {
        val lv = Instance(Definition(new LazyVal()))
        mark(lv.x, lv.y)
      }
      check(new Top(), "~Top|Top/lv:LazyVal>x".rt, "Hi")
    }
    it("3.2: should work on islookupables") {
      class Top() extends MultiIOModule {
        val p = Parameters("hi", 0)
        val up = Instance(Definition(new UsesParameters(p)))
        mark(up.x, up.y.string + up.y.int)
      }
      check(new Top(), "~Top|Top/up:UsesParameters>x".rt, "hi0")
    }
    it("3.3: should work on lists") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasList()))
        mark(i.x(1), i.y(1).toString)
      }
      check(new Top(), "~Top|Top/i:HasList>x_1".rt, "2")
    }
    it("3.4: should work on seqs") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasSeq()))
        mark(i.x(1), i.y(1).toString)
      }
      check(new Top(), "~Top|Top/i:HasSeq>x_1".rt, "2")
    }
    it("3.5: should work on options") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasOption()))
        i.x.map(x => mark(x, "x"))
      }
      check(new Top(), "~Top|Top/i:HasOption>x".rt, "x")
    }
    it("3.6: should work on vecs") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasVec()))
        mark(i.x, "blah")
      }
      check(new Top(), "~Top|Top/i:HasVec>x".rt, "blah")
    }
    it("3.7: should work on statically indexed vectors external to module") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasVec()))
        mark(i.x(1), "blah")
      }
      check(new Top(), "~Top|Top/i:HasVec>x[1]".rt, "blah")
    }
    it("3.8: should work on statically indexed vectors internal to module") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasIndexedVec()))
        mark(i.y, "blah")
      }
      check(new Top(), "~Top|Top/i:HasIndexedVec>x[1]".rt, "blah")
    }
    ignore("3.9: should work on vals in constructor arguments") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new HasPublicConstructorArgs(10)))
        //mark(i.x, i.int.toString)
      }
      check(new Top(), "~Top|Top/i:HasPublicConstructorArgs>x".rt, "10")
    }
  }
  describe("4: Wrapping") {
    it("4.0: should work on modules") {
      class Top() extends MultiIOModule {
        val i = Module(new AddOne())
        f(Instance.wrap(i))
      }
      def f(i: Instance[AddOne]): Unit = mark(i.innerWire, "blah")
      check(new Top(), "~Top|AddOne>innerWire".rt, "blah")
    }
    it("4.1: should work on isinstantiables") {
      class Top() extends MultiIOModule {
        val i = Module(new AddTwo())
        val v = new Viewer(i, false)
        mark(f(Instance.wrap(v)), "blah")
      }
      def f(i: Instance[Viewer]): Data = i.x.i0.innerWire
      check(new Top(), "~Top|AddTwo/i0:AddOne>innerWire".rt, "blah")
    }
    it("4.2: should work on seqs of modules") {
      class Top() extends MultiIOModule {
        val is = Seq(Module(new AddTwo()), Module(new AddTwo())).map(Instance.wrap)
        mark(f(is), "blah")
      }
      def f(i: Seq[Instance[AddTwo]]): Data = i.head.i0.innerWire
      check(new Top(), "~Top|AddTwo/i0:AddOne>innerWire".rt, "blah")
    }
    it("4.3: should work on seqs of isInstantiables") {
      class Top() extends MultiIOModule {
        val i = Module(new AddTwo())
        val vs = Seq(new Viewer(i, false), new Viewer(i, false)).map(Instance.wrap)
        mark(f(vs), "blah")
      }
      def f(i: Seq[Instance[Viewer]]): Data = i.head.x.i0.innerWire
      check(new Top(), "~Top|AddTwo/i0:AddOne>innerWire".rt, "blah")
    }
    it("4.2: should work on options of modules") {
      class Top() extends MultiIOModule {
        val is: Option[Instance[AddTwo]] = Some(Module(new AddTwo())).map(Instance.wrap)
        mark(f(is), "blah")
      }
      def f(i: Option[Instance[AddTwo]]): Data = i.get.i0.innerWire
      check(new Top(), "~Top|AddTwo/i0:AddOne>innerWire".rt, "blah")
    }
  }
  describe("5: Absolute Targets should work as expected") {
    it("5.0: toAbsoluteTarget on a port of an instance") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new AddTwo()))
        amark(i.in, "blah")
      }
      check(new Top(), "~Top|Top/i:AddTwo>in".rt, "blah")
    }
    it("5.1: toAbsoluteTarget on a subinstance's data within an instance") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new AddTwo()))
        amark(i.i0.innerWire, "blah")
      }
      check(new Top(), "~Top|Top/i:AddTwo/i0:AddOne>innerWire".rt, "blah")
    }
    it("5.2: toAbsoluteTarget on a submodule's data within an instance") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new AddTwoMixedModules()))
        amark(i.i1.in, "blah")
      }
      check(new Top(), "~Top|Top/i:AddTwoMixedModules/i1:AddOne_2>in".rt, "blah")
    }
    it("5.3: toAbsoluteTarget on a submodule's data, in an aggregate, within an instance") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new InstantiatesHasVec()))
        amark(i.i1.x.head, "blah")
      }
      check(new Top(), "~Top|Top/i:InstantiatesHasVec/i1:HasVec_2>x[0]".rt, "blah")
    }
    it("5.4: toAbsoluteTarget on a submodule's data, in an aggregate, within an instance, ILit") {
      class MyBundle extends Bundle { val x = UInt(3.W) }
      @instantiable
      class HasVec() extends MultiIOModule {
        @public val x = Wire(Vec(3, new MyBundle()))
      }
      @instantiable
      class InstantiatesHasVec() extends MultiIOModule {
        @public val i0 = Instance(Definition(new HasVec()))
        @public val i1 = Module(new HasVec())
      }
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new InstantiatesHasVec()))
        amark(i.i1.x.head.x, "blah")
      }
      check(new Top(), "~Top|Top/i:InstantiatesHasVec/i1:HasVec_2>x[0].x".rt, "blah")
    }
    it("5.5: toAbsoluteTarget on a subinstance") {
      class Top() extends MultiIOModule {
        val i = Instance(Definition(new AddTwo()))
        amark(i.i1, "blah")
      }
      check(new Top(), "~Top|Top/i:AddTwo/i1:AddOne".it, "blah")
    }
  }
  describe("6: @instantiable traits should work as expected") {
    class MyBundle extends Bundle {
      val in = Input(UInt(8.W))
      val out = Output(UInt(8.W))
    }
    @instantiable
    trait ModuleIntf { self: BaseModule =>
      @public val io = IO(new MyBundle)
    }
    @instantiable
    class ModuleWithCommonIntf extends Module with ModuleIntf {
      @public val sum = io.in + 1.U

      io.out := sum
    }
    class BlackBoxWithCommonIntf extends BlackBox with ModuleIntf

    it("6.0: A Module that implements an @instantiable trait should be instantiable as that trait") {
      class Top extends Module {
        val i: Instance[ModuleIntf] = Instance(Definition(new ModuleWithCommonIntf))
        mark(i.io.in, "gotcha")
      }
      check(new Top, "~Top|Top/i:ModuleWithCommonIntf>io.in".rt, "gotcha")
    }
    it("6.1 An @instantiable Module that implements an @instantiable trait should be able to use extension methods from both") {
      class Top extends Module {
        val i: Instance[ModuleWithCommonIntf] = Instance(Definition(new ModuleWithCommonIntf))
        mark(i.io.in, "gotcha")
        mark(i.sum, "also this")
      }
      val expected = List(
        "~Top|Top/i:ModuleWithCommonIntf>io.in".rt -> "gotcha",
        "~Top|Top/i:ModuleWithCommonIntf>sum".rt -> "also this"
      )
      check(new Top, expected)
    }
    it("6.2 A BlackBox that implements an @instantiable trait should be instantiable as that trait") {
      class Top extends Module {
        val i: Instance[ModuleIntf] = Instance.wrap(Module(new BlackBoxWithCommonIntf))
        mark(i.io.in, "gotcha")
      }
      val expected = List(
        "~Top|BlackBoxWithCommonIntf>in".rt -> "gotcha",
      )
      check(new Top, expected)
    }
  }
  describe("Select api's handle instanceClone properly"){}
}

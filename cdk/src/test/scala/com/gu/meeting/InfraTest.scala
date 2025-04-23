package com.gu.meeting

import collection.mutable.Stack
import org.scalatest._
import flatspec._
import matchers._

class InfraTest extends AnyFlatSpec with should.Matchers {

  "handler method" should "be findable at run time" in {
    InfraStack.handlerMethod should be("handleRequest")
  }

}

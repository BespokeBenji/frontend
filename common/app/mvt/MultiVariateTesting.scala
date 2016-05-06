package mvt

import MultiVariateTesting._
import common.InternationalEditionVariant
import conf.switches.{SwitchGroup, Switch}
import org.joda.time.LocalDate
import play.api.mvc.RequestHeader
import views.support.CamelCase
import conf.switches.Switches.ServerSideTests

// To add a test, do the following:
// 1. Create an object that extends TestDefinition
// 2. Add the object to ActiveTests.tests
//
// object ExampleTest extends TestDefinition(...)
//
// object ActiveTests extends Tests {
//    val tests = List(ExampleTest)
// }

object ABHeadlinesTestVariant extends TestDefinition(
  Nil,
  "headlines-ab-variant",
  "To test how much of a difference changing a headline makes (variant group)",
  new LocalDate(2016, 5, 11)
  ) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.headers.get("X-GU-hlt").contains("hlt-V") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
    }
}

object ABNewHeaderVariant extends TestDefinition(
  variants = Nil,
  name = "ab-new-header-variant",
  description = "Feature switch (0% test) for the new header",
  sellByDate = new LocalDate(2016, 6, 14)
) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.headers.get("X-GU-ab-new-header").contains("variant") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
  }
}

object ABGalleryRedesignVariant extends TestDefinition(
  variants = Nil,
  name = "ab-gallery-redesign-variant",
  description = "Gallery redesign ab test (variant group)",
  sellByDate = new LocalDate(2016, 5, 10)
) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.headers.get("X-GU-ab-gallery-redesign").contains("variant") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
  }
  def shouldShow(contentType: String)(implicit request: RequestHeader): Boolean = {
     isParticipating && contentType.toLowerCase == "gallery"
  }
}

object ABGalleryRedesignControl extends TestDefinition(
  variants = Nil,
  name = "ab-gallery-redesign-control",
  description = "Gallery redesign ab test (control group)",
  sellByDate = new LocalDate(2016, 5, 10)
) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    val doesNotContainGalleryHeader = !request.headers.get("X-GU-ab-gallery-redesign").isDefined

    doesNotContainGalleryHeader && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
  }
}

object ABHeadlinesTestControl extends TestDefinition(
  Nil,
  "headlines-ab-control",
  "To test how much of a difference changing a headline makes (control group)",
  new LocalDate(2016, 5, 11)
  ) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
      request.headers.get("X-GU-hlt").contains("hlt-C") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
    }
}

object ABIntersperseMultipleStoryPackagesStories extends TestDefinition(
  List(Variant8), // 1% of our audience
  "intersperse-multiple-story-packages-stories",
  "To test if mixing storyPackages stories (when article has more than one storyPackage) results in more clicks",
  new LocalDate(2016, 5, 17)
)
object ABIntersperseMultipleStoryPackagesStoriesControl extends TestDefinition(
  List(Variant9), // 1% of our audience
  "intersperse-multiple-story-packages-stories-control",
  "Control for the intersperse-multiple-story-packages-stories A/B test",
  new LocalDate(2016, 5, 17)
)

object ActiveTests extends Tests {
  val tests: Seq[TestDefinition] = List(
    ABNewHeaderVariant,
    ABGalleryRedesignVariant,
    ABGalleryRedesignControl,
    ABHeadlinesTestControl,
    ABHeadlinesTestVariant,
    ABIntersperseMultipleStoryPackagesStories,
    ABIntersperseMultipleStoryPackagesStoriesControl
  )

  def getJavascriptConfig(implicit request: RequestHeader): String = {

    val headlineTests = List(ABHeadlinesTestControl, ABHeadlinesTestVariant).filter(_.isParticipating)
                          .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}
    val newHeaderTests = List(ABNewHeaderVariant).filter(_.isParticipating)
                          .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}
    val galleryRedesignTests = List(ABGalleryRedesignControl, ABGalleryRedesignVariant).filter(_.isParticipating)
                          .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}
    val internationalEditionTests = List(InternationalEditionVariant(request)
                                      .map{ international => s""""internationalEditionVariant" : "$international" """}).flatten

    val activeTest = List(ActiveTests.getParticipatingTest(request)
                        .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}).flatten

    val configEntries = activeTest ++ internationalEditionTests ++ headlineTests ++ newHeaderTests ++ galleryRedesignTests

    configEntries.mkString(",")
  }
}

case class TestDefinition (
  variants: Seq[Variant],
  name: String,
  description: String,
  sellByDate: LocalDate
) {
  val switch: Switch = Switch(
    SwitchGroup.ServerSideABTests,
    name,
    description,
    conf.switches.Off,
    sellByDate,
    exposeClientSide = true
  )

  def isParticipating(implicit request: RequestHeader): Boolean = {
    ActiveTests.getParticipatingTest(request).contains(this)
  }
}

trait Tests {

  protected def tests: Seq[TestDefinition]

  def getParticipatingTest(request: RequestHeader): Option[TestDefinition] = {
    getVariant(request).flatMap { variant =>
      tests.find { test =>
        test.variants.contains(variant) &&
        test.switch.isSwitchedOn &&
        ServerSideTests.isSwitchedOn
      }
    }
  }

  def isParticipatingInATest(request: RequestHeader): Boolean = getParticipatingTest(request).isDefined
}

object MultiVariateTesting {

  sealed case class Variant(name: String)

  // buckets 0-7 are removed during testing whether having a permanently running server side ab test framework
  // affects our caching too much - I'll put them back or come up with a new solution once I have some data! John
  object Variant8 extends Variant("variant-8")
  object Variant9 extends Variant("variant-9")

  private val allVariants = List(
    Variant8, Variant9)

  def getVariant(request: RequestHeader): Option[Variant] = {
    val cdnVariant: Option[String] = request.headers.get("X-GU-mvt-variant")

    cdnVariant.flatMap( variantName => {
      allVariants.find(_.name == variantName)
    })
  }
}

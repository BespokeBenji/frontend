package views.support.cleaner

import com.gu.contentapi.client.model.v1.{Content => ApiContent}
import model.{Article, Content}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{FlatSpec, Matchers}

class AmpEmbedCleanerTest extends FlatSpec with Matchers {

  val googleMapsUrl = "https://www.google.com/maps/embed/v1/place?center=-3.9834936%2C12.7024497&key=AIzaSyBctFF2JCjitURssT91Am-_ZWMzRaYBm4Q&zoom=5&q=Democratic+Republic+of+the+Congo"
  val witnessImgUrl = "https://someurl/media/1234.jpg"
  val witnessAssignment = s"""<html><body><figure class="element element-witness element-witness-image"><div class="element-witness--main><a href="https://witness.theguardian.com/assignment/1234/5678" itemprop="url" data-link-name="in-body-link" class="u-underline"><img src="$witnessImgUrl" alt="Shaka when the walls fell" itemprop="contentURL"></a></div><footer></footer></figure></body></html>"""
  val witnessAssignmentNoImg = s"""<html><body><figure class="element element-witness element-witness-image"><div class="element-witness--main><a href="https://witness.theguardian.com/assignment/1234/5678" itemprop="url" data-link-name="in-body-link" class="u-underline">There be no image here!</a></div><footer></footer></figure></body></html>"""

  private def clean(document: Document): Document = {
    val cleaner = AmpEmbedCleaner(article())
    cleaner.clean(document)
    document
  }

 "AmpEmbedCleaner" should "replace an iframe in an audio-element with an amp-soundcloud element" in {
   val soundcloudUrl = "http://www.soundcloud.com%2Ftracks%2F1234"
   val doc = s"""<html><body><figure class="element-audio"><iframe src="$soundcloudUrl"></iframe></figure></body></html>"""
   val document: Document = Jsoup.parse(doc)
   val result: Document = clean(document)

   result.getElementsByTag("amp-soundcloud").size should be(1)
 }

  "AmpEmbedCleaner" should "not add a amp-soundcloud element if an audio element does not contain an iframe" in {
    val doc = s"""<html><body><figure class="element-audio"></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)

    result.getElementsByTag("amp-soundcloud").size should be(0)
  }

  "AmpEmbedCleaner" should "create an amp-soundcloud element with a trackid from the iframe src" in {
    val trackId = "1234"
    val soundcloudUrl = s"https://www.soundcloud.com%2Ftracks%2F$trackId"
    val doc = s"""<html><body><figure class="element-audio"><iframe src="$soundcloudUrl"></iframe></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)
    result.getElementsByTag("amp-soundcloud").first.attr("data-trackid") should be(trackId)
  }

  "AmpEmbedCleaner" should "not create an amp-soundcloud element if trackid missing from iframe src" in {
    val soundcloudUrl = s"http://www.soundcloud.com/"
    val doc = s"""<html><body><figure class="element-audio"><iframe src="$soundcloudUrl"></iframe></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)

    result.getElementsByTag("amp-soundcloud").size should be(0)
  }

  "AmpEmbedCleaner" should "replace an iframe in an map element with an amp-iframe element" in {
    val doc = s"""<html><body><figure class="element-map"><iframe src="$googleMapsUrl"></iframe></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)

    result.getElementsByTag("amp-iframe").size should be(1)
  }

  "AmpEmbedCleaner" should "not add an amp-iframe element if an map element does not contain an iframe" in {
    val doc = s"""<html><body><figure class="element-map"></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)

    result.getElementsByTag("amp-iframe").size should be(0)
  }

  "AmpEmbedCleaner" should "create an amp-iframe element with an iframe from the iframe src" in {
    val doc = s"""<html><body><figure class="element-map"><iframe src="$googleMapsUrl"></iframe></figure></body></html>"""
    val document: Document = Jsoup.parse(doc)
    val result: Document = clean(document)

    result.getElementsByTag("amp-iframe").first.attr("src") should be(googleMapsUrl)
  }

  "AmpEmbedCleaner" should "replace an img in a witness-image element with an amp-img element" in {
    val document: Document = Jsoup.parse(witnessAssignment)
    val result: Document = clean(document)

    result.getElementsByTag("amp-img").size should be(1)
  }

  "AmpEmbedCleaner" should "not add an amp-img element if a witness-image element does not contain an img" in {
    val document: Document = Jsoup.parse(witnessAssignmentNoImg)
    val result: Document = clean(document)

    result.getElementsByTag("amp-img").size should be(0)
  }

  "AmpEmbedCleaner called on a page with an witness-image element that contains an img-element" should "create an amp-img element with the attributes of the img-element" in {
    val document: Document = Jsoup.parse(witnessAssignment)
    val result: Document = clean(document)

    result.getElementsByTag("amp-img").first.attr("src") should be(witnessImgUrl)
  }



  private def article() = {
    val contentApiItem = contentApi()
    val content = Content.make(contentApiItem)

    Article.make(content)
  }

  private def contentApi() = ApiContent(
    id = "foo/2012/jan/07/bar",
    webTitle = "Some article",
    webUrl = "http://www.guardian.co.uk/foo/2012/jan/07/bar",
    apiUrl = "http://content.guardianapis.com/foo/2012/jan/07/bar"
  )
}

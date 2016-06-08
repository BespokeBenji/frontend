package model

case class SubjectsListing() extends StandalonePage {
  override val metadata = MetaData.make(
    id = "index/subjects",
    section = Some(SectionSummary.fromId("Index")),
    analyticsName = "Subjects",
    webTitle = "subjects",
    customSignPosting = Some(IndexNav.keywordsAlpha))
}

case class ContributorsListing() extends StandalonePage {
  override val metadata = MetaData.make(
    id = "index/contributors",
    section = Some(SectionSummary.fromId("Index")),
    analyticsName = "Contributors",
    webTitle = "contributors",
    customSignPosting = Some(IndexNav.contributorsAlpha))
}

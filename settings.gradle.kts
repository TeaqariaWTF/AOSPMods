pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		maven { setUrl("https://jitpack.io")  }
	}
}

rootProject.name = "PixelXpert"
include(":app")
include(":Submodules:RangeSliderPreference")
include(":annotations")
include(":annotationProcessor")

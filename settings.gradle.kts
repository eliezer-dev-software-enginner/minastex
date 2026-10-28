rootProject.name = "minastex-pedidos"
if (providers.gradleProperty("useLocalSources").orNull != "false") {
    includeBuild("../../framework/libraries/megalodonte-components")
    includeBuild("../../framework/libraries/megalodonte-theme")
}

package berlin.htw.augmentedreality.spatialaudio.messages

data class Authentication(val gameName: String, val id: String, val token: String)
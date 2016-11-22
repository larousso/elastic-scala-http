package com.adelegue.elastic.client

import com.adelegue.elastic.client.api.TypeDefinition

/**
  * Created by adelegue on 19/11/2016.
  */
package object implicits {
  implicit class TypeDefinitionConversion(name: String) {
    def / (`type`: String) = TypeDefinition(name, `type`)
  }
}

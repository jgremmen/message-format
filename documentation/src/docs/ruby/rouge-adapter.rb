# https://github.com/asciidoctor/asciidoctor/issues/4080
class MyRougeAdapter < (Asciidoctor::SyntaxHighlighter.for 'rouge')
  register_for 'rouge'

  def load_library
    require 'rouge'
    require './src/docs/ruby/message.rb'
    :loaded
  end

#  def docinfo? location
#    false
#  end
end

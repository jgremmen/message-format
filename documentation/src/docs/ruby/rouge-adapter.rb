# https://github.com/asciidoctor/asciidoctor/issues/4080
class MyRougeAdapter < (Asciidoctor::SyntaxHighlighter.for 'rouge')
  register_for 'rouge'

  def load_library
    require 'rouge'
    require './src/docs/ruby/message.rb'
    :loaded
  end

  def docinfo location, doc, opts
    stylesheet = doc.attr 'rouge-stylesheet', './src/docs/css/rouge.css'
    if opts[:linkcss]
      slash = opts[:self_closing_tag_slash]
      %(<link rel="stylesheet" href="#{stylesheet}"#{slash}>)
    else
      stylesheet = doc.normalize_system_path stylesheet
      %(<style>
#{doc.read_asset stylesheet, label: 'stylesheet', normalize: true}
</style>)
    end
  end
end

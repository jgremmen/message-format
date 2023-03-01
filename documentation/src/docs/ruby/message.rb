# -*- coding: utf-8 -*- #
# frozen_string_literal: true

module Rouge
  module Lexers
    class MessageFormat < RegexLexer
      title 'MESSAGE'
      desc "Message Format"
      tag 'msg'

      nameFragment = /[a-zA-Z][a-zA-Z0-9_]*/
      dashedName = /#{nameFragment}(?:-#{nameFragment})*/
      number = /-?[0-9]+/
      operator = /<>|<=|<|>=|>|!|=/

      state :root do
        mixin :string
        mixin :parameterStart
        rule %r/['"%]/, Text
      end

      # string matching stops at ', " or %
      state :string do
        rule %r/[^'"\\%]+/, Text
        rule %r/\\u[0-9a-fA-F]{4,4}/, Str::Escape
        rule %r/\\["'%{\\]/, Str::Escape
        rule %r/\\/, Text
      end

      state :sq_string do
        mixin :string
        rule %r/'/, Str::Single, :pop!
        rule %r/["%]/, Text
      end

      state :dq_string do
        mixin :string
        rule %r/"/, Str::Double, :pop!
        rule %r/['%]/, Text
      end

      state :whitespace do
        rule %r/\s+/, Text::Whitespace
      end

      state :parameter do
        mixin :whitespace
        rule %r/#{dashedName}/ do
          token Name::Variable
          goto :parameterFormat
        end
      end

      state :parameterFormat do
        mixin :whitespace
        rule %r/(,)(\s*)(#{dashedName})/ do
          groups Punctuation, Text::Whitespace, Name::Namespace
          goto :parameterConfigKey
        end
        mixin :parameterConfigKey
      end

      state :parameterConfigKey do
        mixin :parameterEnd
        rule %r/(,)(\s*)(#{operator}?)(true|false|null|empty)(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Operator, Keyword::Constant,
                 Text::Whitespace, Punctuation
          goto :parameterConfigValue
        end
        rule %r/(,)(\s*)(#{dashedName})(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Name::Label, Text::Whitespace, Punctuation
          goto :parameterConfigValue
        end
        rule %r/(,)(\s*)(#{operator}?)(\s*)(#{number})(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Operator, Text::Whitespace,
                 Literal::Number, Text::Whitespace, Punctuation
          goto :parameterConfigValue
        end
        rule %r/(,)(\s*)(#{operator}?)(\s*)(')/ do
          groups Punctuation, Text::Whitespace, Operator, Text::Whitespace, Str::Single
          goto :parameterConfigValue
          push :sq_string
        end
        rule %r/(,)(\s*)(#{operator}?)(\s*)(")/ do
          groups Punctuation, Text::Whitespace, Operator, Text::Whitespace, Str::Double
          goto :parameterConfigValue
          push :dq_string
        end
        rule %r/(,)(\s*)(:)(\s)*(')/ do
          groups Punctuation, Text::Whitespace, Punctuation, Text::Whitespace, Str::Single
          goto :parameterConfigKey
          push :sq_message
        end
        rule %r/(,)(\s*)(:)(\s)*(")/ do
          groups Punctuation, Text::Whitespace, Punctuation, Text::Whitespace, Str::Double
          goto :parameterConfigKey
          push :dq_message
        end
      end

      state :parameterConfigValue do
        mixin :parameterEnd
        rule %r/true|false/ do
          token Keyword::Constant
          goto :parameterConfigKey
        end
        rule %r/#{number}/ do
          token Number
          goto :parameterConfigKey
        end
        rule %r/#{dashedName}/ do
          token Str
          goto :parameterConfigKey
        end
        rule %r/'/ do
          token Str::Single
          goto :parameterConfigKey
          push :sq_message
        end
        rule %r/"/ do
          token Str::Double
          goto :parameterConfigKey
          push :dq_message
        end
      end

      state :parameterStart do
        rule %r/%{/, Keyword::Variable, :parameter
      end

      state :parameterEnd do
        mixin :whitespace
        rule %r/}/, Keyword::Variable, :pop!
      end

      state :sq_message do
        mixin :string
        mixin :parameterStart
        rule %r/'/, Str::Single, :pop!
        rule %r/["%]/, Text
      end

      state :dq_message do
        mixin :string
        mixin :parameterStart
        rule %r/"/, Str::Double, :pop!
        rule %r/['%]/, Text
      end
    end
  end
end

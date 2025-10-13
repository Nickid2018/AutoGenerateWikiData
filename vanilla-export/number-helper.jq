# https://rosettacode.org/wiki/Non-decimal_radices/Convert#jq
def convert(base):
  def stream:
    recurse(if . >= base then ./base|floor else empty end) | . % base ;
  [stream] | reverse
  | if   base <  10 then map(tostring) | join("")
    elif base <= 36 then map(if . < 10 then 48 + . else . + 87 end) | implode
    else error("base too large")
    end;

def lpad($len; $fill):
  tostring |
  ($len - length) as $l |
  ($fill * $l)[:$l] + .;
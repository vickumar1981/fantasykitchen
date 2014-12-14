$(document).ready(function(){

    // Add search/filtering on Customers page (highlighting)
    $(document).ready(function() {
      $('#instant-filter input#instant-filter-input').keyup(function() {

        var sq = $(this).val().toLowerCase();

        $("table#order-list tr").each(function(){
          var current_text = $(this).find('td.order-date,td.order-id,td.order-total').text().toLowerCase();
          if (sq.length > 0) {
            if (current_text.indexOf(sq) >= 0) {
              $(this).css('background', '#f0f0e9');
            } else {
              $(this).css('background', 'inherit');
            }
          } else {
            $(this).css('background', 'inherit');
          }
        });

      });
    });
});
